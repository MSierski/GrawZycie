import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


class Komorka extends Thread {
    private int value = 0;
    private int row = 0;
    private int column = 0;
    private int nextGenerationValue = -1;
    private Mapa Mapa = null;
    private ThreadMonitor threadmonitor = null;
    
    public Komorka(final Mapa Mapa, final int row, final int column, final int value) {
        super("Wątek");
        this.Mapa = Mapa;
        this.row = row;
        this.column = column;
        this.value = value;
    }
    
    public final void setThreadMonitor(ThreadMonitor threadMonitor) {
        this.threadmonitor = threadMonitor;
    }
    
    public final void run() {
        if (threadmonitor == null) {
            throw new GameLogicException("Wątek musi mieć ThreadMonitor.");
        }
        
        while (!isInterrupted()) {
            playGame();
        }
        
        System.out.println(getName() + " [" + row + "," + column + "] "+ "zamknięty.");
        
    }

    private final synchronized void playGame() {
        calculateNextGenerationValue();
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        setNextGenerationValue();
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public final synchronized void Kontynuuj() {
        notify();
    }
    
    public final synchronized void Zatrzymaj() {
        notify();
        interrupt();
    }

    private void calculateNextGenerationValue() {
        nextGenerationValue = getNextGenerationValue();
        threadmonitor.ZakKalkulacji();
    }

    public final int getNextGenerationValue() {
        if (value == 1) {
            return getNextGenerationValueForOne();
        } else {
            
            return getNextGenerationValueForZero();
        }
    }

    private int getNextGenerationValueForOne() {

        int neighborsAmount = IleSasiadow(1);
        if (neighborsAmount < 2 || neighborsAmount > 3) {
            return 0;
        }
        
        return 1;
    }

    private int getNextGenerationValueForZero() {

        int neighborsAmount = IleSasiadow(1);
        if (neighborsAmount == 3) {
            return 1;
        }
        
        return 0;
    }
    

    private int IleSasiadow(int neighborValue) {
        int amount = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = column - 1; j <= column + 1; j++) {
                Komorka neighbor = Mapa.OdczytajKomorke(i, j);
                
                if (neighbor == this) {
                    continue; // Do not count this.
                }
                
                if (neighbor != null) {
                    if (neighbor.getValue() == neighborValue) {
                        amount++;
                    }
                }
            }
        }
        
        return amount;
    }

    private final void setNextGenerationValue() {
        if (nextGenerationValue != 0 && nextGenerationValue != 1) {
            throw new GameLogicException(" Wartość przyjeła wartość inną niz 0 lub 1.");
        }
        
        value = nextGenerationValue;
        nextGenerationValue = -1;
        threadmonitor.threadFinishedNextValueSet();
    }

    public final int getValue() {
        return value;
    }

    public final void setValue(final int newValue) {
        if (newValue != 0 && newValue != 1) {
            throw new GameLogicException("Wartość komórki musi wynosić 0 lub 1 a nie " + " " + newValue + ".");
        }
        value = newValue;
    }

    public final int getRow() {
        return row;
    }
    
    public final int getColumn() {
        return column;
    }
}

class Mapa {
    
    private Komorka[][] Mapa; // rows, columns
    private int columns = 0;
    private int rows = 0;
    
    public Mapa(final int rows, final int columns) {
        this.columns = columns;
        this.rows = rows;
        StworzKomorki();
    }

    /**
     * @param stringRows And array of strings containing values 0 or 1.
     */
    public void WczytajWartosci(List<String> stringRows) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                char character = stringRows.get(i).charAt(j);
                int value = Integer.valueOf(String.valueOf(character));
                if (value != 0 && value != 1) {
                    throw new GameLogicException(
                            "Input array must contain only values 1 or 0. Found value" + " " + value + ".");
                }
                
                Mapa[i][j] = new Komorka(this, i + 1, j + 1, value);
            }
        }
    }
    
    public void StworzKomorki() {
        Mapa = new Komorka[rows][columns];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Mapa[i][j] = new Komorka(this, i + 1, j + 1, 0);
            }
        }
    }

    public final void WczytajMape() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                System.out.print(Mapa[i][j].getValue());
            }
            
            System.out.print("\n");
        }
    }
    
    public void UstawWartoscKomorki(final int row, final int column, final int newValue) {
        OdczytajKomorke(row, column).setValue(newValue);
    }
    
    public final Komorka OdczytajKomorke(final int row, final int column) {
        if (row > rows || row < 1) {
            return null;
        }
        
        if (column > columns || column < 1) {
            return null;
        }
        
        Komorka Komorka = Mapa[row - 1][column - 1];
        
        if (Komorka.getRow() != row || Komorka.getColumn() != column) {
            throw new RuntimeException("Komorka nie była tam gdzie powinna.");
        }
        
        return Komorka;
    }
    
    public final List<Komorka> getKomorki() {
        ArrayList<Komorka> Komorki = new ArrayList<>();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Komorki.add(Mapa[i][j]);
            }
        }
        
        return Komorki;
    }
    
    public final int getRows() {
        return rows;
    }
    
    public final int getColumns() {
        return columns;
    }
    
}

class ThreadMonitor extends Thread {
    
    private Mapa gameSessionMapa = null;
    private ArrayList<Komorka> Komorki = null;
    private int IloscGen = 10;
    private int tmp1 = 0;
    private int tmp2 = 0;
    

    
    public ThreadMonitor(final Mapa Mapa) {
        this.gameSessionMapa = Mapa;
    }
    
    public final void setIloscGen(final int IloscGen) {
        this.IloscGen = IloscGen;
    }
    
    public final void Start() {
        setKomorkiThreadMonitor();
        startKomorkaThreads();
        computeGenerations();
    }
    


    private void setKomorkiThreadMonitor() {
        Komorki = (ArrayList<Komorka>) gameSessionMapa.getKomorki();
        for (Komorka Komorka : Komorki) {
            Komorka.setThreadMonitor(this);
        }
    }
    
    private void startKomorkaThreads() {
        for (Komorka Komorka : Komorki) {
            Komorka.start();
        }
    }
    
    private synchronized void computeGenerations() {

        for (int i = 1; i <= IloscGen; i++) {
            Pauza1();           
            wznowWatek();
            Pauza2();
            
            wyswietlGen(i);

            

            if (i < IloscGen) {
                wznowWatek(); 
            }
        }
        
        ZatrzymajWatek();
        wznowWatek(); 

    }
   
    private void Pauza1() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void Pauza2() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void wznowWatek() {
        for (Komorka Komorka : Komorki) {
            Komorka.Kontynuuj();
        }
    }

    private void wyswietlGen(int nrGen) {
        System.out.println("---------- " + "Generacja" + " " + nrGen + " ----------");
        gameSessionMapa.WczytajMape();
    }
      
    private void ZatrzymajWatek() {
        for (Komorka Komorka : Komorki) {
            Komorka.Zatrzymaj();
        }
    }

    public final synchronized void ZakKalkulacji() {
        tmp1++;
        
        if (tmp1 == Komorki.size()) {
            tmp1 = 0;
            notify();
        }
    }
    
    public final synchronized void threadFinishedNextValueSet() {
        tmp2++;
        
        if (tmp2 == Komorki.size()) {
            tmp2 = 0;
            notify();
        }
    }

}

class GameSession {
    
    private Mapa Mapa = null;
    private ThreadMonitor threadMonitor = null;
    public void StartSim(final String inputFile, final int IloscGen) throws IOException {
        StworzMape(inputFile, IloscGen);
        createThreadMonitor(IloscGen);
        Start();
    }
    //stworzenie listy stringów z Mapay txt
    private void StworzMape(final String inputFile, final int IloscGen) throws IOException {
        List<String> rows = Plik(inputFile);
        Mapa = new Mapa(rows.size(), rows.get(0).length());
        Mapa.WczytajWartosci(rows);
        
        System.out.println("Zaimportowano Mapaę" + ":");
        Mapa.WczytajMape();
    }

    private List<String> Plik(final String inputFile) throws IOException {
        ArrayList<String> rows = new ArrayList<>();
        
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
        try {
            String line = bufferedReader.readLine();

            while (line != null) {
                rows.add(line);
                line = bufferedReader.readLine();
            }
        } finally {
            bufferedReader.close();
        }
        
        return rows;
    }

    private void createThreadMonitor(int IloscGen) {
        threadMonitor = new ThreadMonitor(Mapa);
        threadMonitor.setIloscGen(IloscGen);
    }

    private void Start() {
        System.out.println("Uruchamianie symulacji...");
        threadMonitor.Start();
    }
    
}

 class GameLogicException extends RuntimeException {
    public GameLogicException(final String message) {
        super(message);
    }
}

 class GraWZycie {
    private static GameSession gameSession = null;
    public static void run(final String inputFile, final int IloscGen) throws IOException {
        System.out.println("Uruchamianie symulacji...");
        gameSession = new GameSession();
        gameSession.StartSim(inputFile, IloscGen);
        System.out.println("Zatrzymywanie symulacji.");
    }    
}

public class App {
    public static void main(String[] args) {
        Scanner reader = new Scanner(System.in);
        System.out.println("Ile generacji ma zasymulować program?");
        int n = reader.nextInt();
        try {
                GraWZycie.run("Mapa.txt", n);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
