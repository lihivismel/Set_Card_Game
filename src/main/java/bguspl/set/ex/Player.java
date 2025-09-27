package bguspl.set.ex;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue ;  

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    //שלנו
    public int usedTokens; //returns how mant tokens left to the player

    public int[] placedTokens; //represent where this player have tokens already

    private BlockingQueue<Integer> actions; // last player's actions

    public BlockingQueue<Integer> ans; //answer if the set is true=1 or false=0
    
    public boolean checked=false;

   public boolean set=false;


    private final Dealer dealer;


    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer=dealer;
        this.playerThread = new Thread (this, "player");
        this.terminate=false;
        this.placedTokens= new int[3];
        for(int i=0;i<placedTokens.length;i++)
            placedTokens[i]=-1;
        usedTokens=0;
        this.actions = new ArrayBlockingQueue<>(3);
        this.ans = new ArrayBlockingQueue<>(1);

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {

            // TODO implement main player loop
            // while (!dealer.replace){

                act(); // place or remove token

                if(usedTokens==3 && !checked ){

                    int [] tokens = placedTokens;
                    Integer []tocheck = {id, tokens[0], tokens[1], tokens[2]};
                    try{
                        dealer.setsToCheck.put(tocheck);
                    } catch (InterruptedException ex) {}
                    synchronized(dealer.lock){
                        try{
                            dealer.lock.notifyAll();
                        } catch (IllegalMonitorStateException ex) {}

                    }    

                    Integer answer=0;
                    //if(dealer.isSet(id, placedTokens)){
                    try{
                        answer =ans.take();
                    } catch (InterruptedException ex) {}

                    if (answer==1){
                    // dealer.existingSet=true;
                        for(int i=0;i<placedTokens.length;i++)
                            {placedTokens[i]=-1;}
                        usedTokens=0;
                        env.logger.info("there is a set");
                        point();
                    }
                    else {
                        penalty();
                        removeAllTokens();

                
                    }
                checked=true;
                }
        }
    

        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    
    }
    

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                
                boolean placed=false;

                int slot = (int)((Math.random()*table.slotToCard.length));
                
                if(table.slotToCard[slot]!=null) {
                    for(int k=0;k<placedTokens.length && !placed ;k++){
                        if(placedTokens[k]==slot){
                            placed=true;
                            placedTokens[k]=-1;
                            table.removeToken(this.id,slot);
                            usedTokens--;
                        }
                    }
                        
                    if(!placed ){
                        boolean isIn=false;
                        for(int i=0;i<placedTokens.length&&(!isIn);i++){
                            if(placedTokens[i]==-1){
                                placedTokens[i]=slot;
                                isIn=true;
                                table.placeToken(this.id,slot);
                                usedTokens++;
                                if(usedTokens==3){
                                    checked=false;
                                }
                            }
                        }
                    }
            }
        
        

            // act(); // place or remove token
            if(usedTokens==3){
                int [] tokens = placedTokens;
                Integer []tocheck = {id, tokens[0], tokens[1], tokens[2]};
                try{
                    dealer.setsToCheck.put(tocheck);
                } catch (InterruptedException ex) {}
                synchronized(dealer.lock){
                    try{
                        dealer.lock.notifyAll();
                    } catch (IllegalMonitorStateException ex) {}

                }    

                Integer answer=0;
                    
                try{
                    answer =ans.take();
                } catch (InterruptedException ex) {}

                if (answer==1){
                    point();
                    env.logger.info("there is a set");}
                else{penalty(); }
                removeAllTokens();

            }  
            

            try {
                Thread.sleep(200); //  to see more clear the actions of the AI
            } catch (InterruptedException ignored) {}
        }
        
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        if(human) {playerThread.interrupt();}
        else {aiThread.interrupt();}
        terminate=true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        try {
            actions.put(slot);
        } catch (InterruptedException ignored) {}
    }

   

    // activate the kepPressed action - put or remove a token
    public void act(){
            Integer slot=null;
            boolean placed=false;

            try {
                slot = actions.take();
            } catch (InterruptedException ignored) {}
            
            if ((slot!=null) && table.slotToCard[slot]!=null) {
                for(int k=0;k<placedTokens.length && !placed ;k++){
                    if(placedTokens[k]==slot){
                        placed=true;
                        placedTokens[k]=-1;
                        table.removeToken(this.id,slot);
                        usedTokens--;
                    }
                }
                    
                if(!placed ){
                    boolean isIn=false;
                    for(int i=0;i<placedTokens.length&&(!isIn);i++){
                        if(placedTokens[i]==-1){
                            placedTokens[i]=slot;
                            isIn=true;
                            table.placeToken(this.id,slot);
                            usedTokens++;
                            if(usedTokens==3){
                                checked=false;
                            }
                        }
                    }
                }
            }
        
    }  
            
      

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        long millies = env.config.pointFreezeMillis;
        int rounds = (int)(env.config.pointFreezeMillis)/1000;
        for(int i=0; i<rounds; i++){
            env.ui.setFreeze(id, millies);
            millies = millies-1000;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
        if (millies!= 0){
            try {
                Thread.sleep(millies);
            } catch (Exception e) {}
            env.ui.setFreeze(id, millies);
        }
        env.ui.setFreeze(id, 0);
        emptyQueue();
   

    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long millies = env.config.penaltyFreezeMillis;
        int rounds = (int)(env.config.penaltyFreezeMillis)/1000;
        for(int i=0; i<rounds; i++){
            env.ui.setFreeze(id, millies);
            millies = millies-1000;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
        if (millies!= 0){
            try {
                Thread.sleep(millies);
            } catch (Exception e) {}
            env.ui.setFreeze(id, millies);
        }
        env.ui.setFreeze(id, 0);
        emptyQueue();
    }
   

    public int score() {
        return score;
    }

    //שלנו
    public void StartPlayerThread(){
        playerThread.start();

    }

    //שלנו
    public int[] getTokens(){
        return placedTokens;
    }

    //שלנו
    public void removeToken(int card){
        for(int i=0;i<3;i++){
            if(placedTokens[i]==card){
                placedTokens[i]=-1;
                table.removeToken(id,table.cardToSlot[card]);
            }
        }
    }
    public void removeAllTokens(){
        for(int i=0;i<3;i++){
            if(placedTokens[i]!=-1)
                {table.removeToken(id, placedTokens[i]);}
        }
        for(int i=0;i<placedTokens.length;i++)
            placedTokens[i]=-1;
        usedTokens=0;
    }

    //שלנו
    public int getId(){
        return id;
    }

    public void emptyQueue(){
        while(!actions.isEmpty()){
            actions.remove();
        }

    }
}
