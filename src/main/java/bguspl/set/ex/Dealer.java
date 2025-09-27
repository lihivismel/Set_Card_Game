package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.BlockingQueue ; 

//שלנו
import java.util.Iterator;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    public int freeze=0;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;
    public boolean replace= false;
    

    

    public  boolean threeTokens=false;
    public boolean existingSet=false; //true if there is the dealer found a set on the tableject
    Object lock = new Object();

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    private int [] setOfCards = {-1,-1,-1}; // שלנו
    private int [] setOfSlots = {-1,-1,-1}; // שלנו
    public BlockingQueue<Integer[]> setsToCheck; // holds ids of players that claim they have set


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.setsToCheck = new ArrayBlockingQueue<Integer[]>(players.length);

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

        for(int i=0;i<players.length;i++){
            players[i].StartPlayerThread();
        }

        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            timerLoop();
            emptyQueue(setsToCheck);
            removeAllCardsFromTable();

        }
        announceWinners();
        terminate();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        

        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout(); //Sleep for a fixed amount of time or until the thread is awakened for some purpose.
            updateTimerDisplay(false);

            sentToIsSet();
            if(existingSet){
                // replace=true;
                synchronized(table){
                removeCardsFromTable();
                updateTimerDisplay(true);
                placeCardsOnTable();
                }
                // replace=false;
            }
            existingSet=false;

    }

}

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement

        for(int i=0;i<players.length;i++){
            players[i].terminate();
        }
        terminate=true;

    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    //שלנו
    public boolean isSet(int playerId, int []placedTokens){
        boolean isSet=false;
        
            if (table.slotToCard[placedTokens[0]]!=null&&table.slotToCard[placedTokens[1]]!=null&&table.slotToCard[placedTokens[2]]!=null){
                setOfSlots[0]=placedTokens[0];
                setOfSlots[1]=placedTokens[1];
                setOfSlots[2]=placedTokens[2];
                setOfCards[0]=table.slotToCard[placedTokens[0]];
                setOfCards[1]=table.slotToCard[placedTokens[1]];
                setOfCards[2]=table.slotToCard[placedTokens[2]];
                isSet=env.util.testSet(setOfCards);
            }
            for(int i=0; i<3; i++){
                if(table.slotToCard[placedTokens[i]]==null){
                    table.removeToken(setOfSlots[i]);
                    players[playerId].placedTokens[i]=-1;
                }
            }


            
            if(isSet){
                try{
                    players[playerId].ans.put(1);}
                catch(InterruptedException ex){};
                existingSet=true;
                players[playerId].set=true;
                table.removeToken(setOfSlots[0]);
                table.removeToken(setOfSlots[1]);
                table.removeToken(setOfSlots[2]);
                
            }
            else{
                try{
                    players[playerId].ans.put(0);}
                catch(InterruptedException ex){};

            }
        
        threeTokens=false;
        return isSet;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void  removeCardsFromTable () {
        // TODO implement

        for(int k=0;k<setOfSlots.length;k++){
            table.removeCard(setOfSlots[k]);
            
        }
        setOfCards[0]=-1;
        setOfCards[1]=-1;
        setOfCards[2]=-1;

    }
    

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
   
        for(int i=0;i<table.slotToCard.length && (deck.size()>0);i++){
                int rendomIndex= (int)(Math.random()*deck.size());
                if(table.slotToCard[i]==null){
                    int card= deck.remove(rendomIndex);
                    table.placeCard(card,i);
                }
                        
        }
        
        
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        synchronized(lock){
        try{
            if(reshuffleTime - System.currentTimeMillis() >=env.config.turnTimeoutWarningMillis){
                lock.wait(1000);}
            
        }
        catch(InterruptedException ex) {}
    }
}

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        
        if(reset){
            reshuffleTime=System.currentTimeMillis() + env.config.turnTimeoutMillis+500;
            env.ui.setCountdown( env.config.turnTimeoutMillis, false);
        }
        else{
            if(((reshuffleTime - System.currentTimeMillis()) <env.config.turnTimeoutWarningMillis) && (reshuffleTime > System.currentTimeMillis())){
                    env.ui.setCountdown( reshuffleTime - System.currentTimeMillis() , true);
            }
            else{
                env.ui.setCountdown( reshuffleTime - System.currentTimeMillis(), false);

                }
                
        }
    }


    /**
     * Returns all the cards from the table to the deck.
     */
    private synchronized void removeAllCardsFromTable() {
        // TODO implement
    
        
        for(int i=0;i<table.slotToCard.length;i++){   //remove the cards
            Integer card = table.slotToCard[i];
            if (card != null){
                table.removeCard(i);
                deck.add(card);
            }
        }

        for(int p=0;p<players.length;p++){
            players[p].removeAllTokens();
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement

        LinkedList<Player> winners= new LinkedList<>();
        int maxScore=-1;
        for(int i=0; i<players.length; i++){
            if(players[i].score() > maxScore)
                maxScore = players[i].score();
        }
        for(int i=0; i<players.length; i++){
            if(players[i].score() == maxScore){
                winners.add(players[i]);
            }
        }

        int[] winnersIds=new int[winners.size()];
        Iterator<Player> it = winners.iterator();
        for(int i=0;i<winners.size()&&(it.hasNext());i++){
            winnersIds[i]= it.next().getId();

        }
        env.ui.announceWinner(winnersIds);
    }

    //שלנו
    public Player getPlayer(int id){
        for(int i=0;i<players.length;i++){
            if(players[i].id==id){
                return players[i];
            }
        }
        return null;
    }

    public void threeTokens(){
        threeTokens=true;
        notifyAll();
    }

    public void sentToIsSet(){

        if(!setsToCheck.isEmpty()){
            
            Integer[] arr= setsToCheck.poll();
            int id=arr[0];
            int[]tokens={arr[1],arr[2],arr[3]};
            isSet(id, tokens);
        } 
    }

    public void emptyQueue(BlockingQueue<Integer[]> queue){
        while(!queue.isEmpty()){
            queue.remove();
        }

    }
    public void interrupt (){
        Thread.currentThread().interrupt();
    }
        
}


    

    

