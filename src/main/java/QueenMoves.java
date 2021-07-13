import java.awt.datatransfer.Clipboard;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.BoardEventType;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import com.github.bhlangonijr.chesslib.pgn.PgnIterator;
import com.github.bhlangonijr.chesslib.util.LargeFile;

/**
 * QueenMoves
 */
public class QueenMoves {
    
    public static final long MAX_GAMES = 1000000; //Long.MAX_VALUE; //1000000;
    public static final int MAX_THREADS = 10;
    private long queenMoves = 0;
    private long games = 0;
    private long diagonals = 0;
    private long files = 0;
    private long ranks = 0;
    private long start = System.currentTimeMillis();
    
    private final String filename;
    
    private TransferQueue<Game> gameQ = new LinkedTransferQueue<>();
    
    private List<Thread> workers = new ArrayList<>();
    
    private class GameWorker implements Runnable {
        @Override
        public void run() {
            System.out.print("+");
            while (true) {
                try {
                    long queenMoves = 0;
                    long ranks = 0;
                    long files = 0;
                    long diagonals = 0;
                    Game game = gameQ.take();
                    game.loadMoveText();
                    MoveList moves = game.getHalfMoves();
                    Board board = new Board();
                    for (Move move: moves) {
                        Square from = move.getFrom();
                        Square to = move.getTo();
                        Piece piece = board.getPiece(from);
                        board.doMove(move);
                        if (Piece.WHITE_QUEEN.equals(piece) || Piece.BLACK_QUEEN.equals(piece)) {
                            queenMoves++;
                            Rank fromRank = from.getRank();
                            File fromFile = from.getFile();
                            Rank toRank = to.getRank();
                            File toFile = to.getFile();
                            if (fromRank.equals(toRank)) {
                                ranks++;
                            } else if (fromFile.equals(toFile)) {
                                files++;
                            } else {
                                diagonals++;
                            }
                        }
        
                    }
                    register (queenMoves, ranks, files, diagonals);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }
    
    QueenMoves(String filename) {
        this.filename = filename;
    }
    
    private synchronized void register (long queenMoves, long ranks, long files, long diagonals) {
        this.games++;
        this.queenMoves += queenMoves;
        this.ranks += ranks;
        this.files += files;
        this.diagonals += diagonals;
    
        if (this.games % 100000 == 0) {
            System.out.print("\n" + this.games + ":" + gameQ.size() + ":" + gameQ.getWaitingConsumerCount() + "/" + workers.size() + " ");
        }
        if (this.games % 10000 == 0) {
            System.out.print(".");
        }
    }
    
    private void startWorker() {
        if (workers.size() < MAX_THREADS) {
            Thread t = new Thread(new GameWorker(), "Worker " + (workers.size() + 1));
            workers.add(t);
            t.start();
        }
    }
    
    private void endWorkers() {
        for (Thread t : workers) {
            t.interrupt();
        }
    }
    
    private void start() throws Exception {
        
        PgnIterator gamesFile = new PgnIterator(filename);
        int gamecount = 0;
        for (Game game : gamesFile) {
            gamecount ++;
            if (gameQ.getWaitingConsumerCount() == 0) {
                startWorker();
            }
            gameQ.put(game);
            if (gamecount >= MAX_GAMES) {
                break;
            }
        }
        
        while(!gameQ.isEmpty() || this.games < gamecount) {
            Thread.sleep(500);
        }
        
        output();
        
        endWorkers();
    }
    
    private synchronized void output() {
        System.out.println();
        double moveFactor = (100d / queenMoves);
        System.out.println("Games:" + games);
        System.out.println("Queen Moves: " + queenMoves);
        System.out.println("Along Diagonals:" + diagonals + " (" + NumberFormat.getNumberInstance().format(moveFactor * diagonals) + "%)");
        System.out.println("Along Ranks:" + ranks+ " (" + NumberFormat.getNumberInstance().format(moveFactor * ranks) +
            "%)");
        System.out.println("Along Files:" + files + " (" + NumberFormat.getNumberInstance().format(moveFactor * files) + "%)");
    
        long duration = System.currentTimeMillis() - start;
        long hours = duration / (1000 * 60 * 60);
        duration %= (1000*60*60);
        long minutes = duration / (1000*60);
        duration %= (1000*60);
        double seconds = duration / 1000d;
    
        System.out.printf("Duration %d hours, %d minutes, %.2f seconds", hours, minutes, seconds);
    }
    
    public static void main(String[] args) throws Exception {
        //QueenMoves moves = new QueenMoves("/home/uluebke/stuff/chess/Nakamura.pgn");
        QueenMoves moves = new QueenMoves("/home/uluebke/stuff/chess/lichess_db_standard_rated_2021-06.pgn");
        moves.start();
    }
    /*
    public static void main(String[] args) throws Exception {
        LinkedTransferQueue<>
        long queenMoves = 0;
        long games = 0;
        long diagonals = 0;
        long files = 0;
        long ranks = 0;
        long start = System.currentTimeMillis();
        System.out.println("Hallo!");
        //PgnIterator gamesFile = new PgnIterator(new LargeFile("/home/uluebke/stuff/chess/Nakamura.pgn"));
        PgnIterator gamesFile = new PgnIterator(new LargeFile("/home/uluebke/stuff/chess/lichess_db_standard_rated_2021-06.pgn"));
        
        for (Game game: gamesFile) {
            game.loadMoveText();
            games++;
            MoveList moves = game.getHalfMoves();
            Board board = new Board();
            for (Move move: moves) {
                Square from = move.getFrom();
                Square to = move.getTo();
                Piece piece = board.getPiece(from);
                board.doMove(move);
                if (Piece.WHITE_QUEEN.equals(piece) || Piece.BLACK_QUEEN.equals(piece)) {
                    queenMoves++;
                    Rank fromRank = from.getRank();
                    File fromFile = from.getFile();
                    Rank toRank = to.getRank();
                    File toFile = to.getFile();
                    if (fromRank.equals(toRank)) {
                        ranks++;
                    } else if (fromFile.equals(toFile)) {
                        files++;
                    } else {
                        diagonals++;
                    }
                }
                
            }
            if (games % 100000 == 0) {
                System.out.print("\n" + games + " ");
            }
            if (games % 1000 == 0) {
                System.out.print(".");
            }
            //System.out.println("FEN: " + board.getFen());
            if (games >= MAX_GAMES) {
                break;
            }
        }
        System.out.println();
        double moveFactor = (100d / queenMoves);
        System.out.println("Games:" + games);
        System.out.println("Queen Moves: " + queenMoves);
        System.out.println("Along Diagonals:" + diagonals + " (" + NumberFormat.getNumberInstance().format(moveFactor * diagonals) + "%)");
        System.out.println("Along Ranks:" + ranks+ " (" + NumberFormat.getNumberInstance().format(moveFactor * ranks) +
            "%)");
        System.out.println("Along Files:" + files + " (" + NumberFormat.getNumberInstance().format(moveFactor * files) + "%)");
        
        long duration = System.currentTimeMillis() - start;
        long hours = duration / (1000 * 60 * 60);
        duration %= (1000*60*60);
        long minutes = duration / (1000*60);
        duration %= (1000*60);
        double seconds = duration / 1000d;
        
        System.out.printf("Duration %d hours, %d minutes, %.2f seconds", hours, minutes, seconds);
    }
    
        duration %= (1000*60);
        double seconds = duration / 1000d;
        
        System.out.printf("Duration %d hours, %d minutes, %.2f seconds", hours, minutes, seconds);
        
     */
}
