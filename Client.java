
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client class
 *
 * @author Zahar Adiniaev, Yosi Gavriel, Aviya pinhas
 */
public class Client {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int port;
    private String host;
    private int fileName;
    private int R1, R2;
    private ArrayList<Double> proList = null;
    private int numsArr[] = null;
    private int query;
    private int clientId;

    /**
     *
     * @param fileName the probability file name
     */
    public Client(int fileName) {///to remove int ID

        this.clientId = 0;
        this.port = 55000;
        this.host = "localhost";
        this.proList = new ArrayList<>();
        this.fileName = fileName;

        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);//whrite to the socket
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//read from the socket
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

        new Thread(new Client.clientThread(socket)).start();
    }

    class clientThread implements Runnable {

        Socket clientSocket = null;

        public clientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

        }

        @Override
        public void run() {
            readFile(fileName);
            numsArr = probabilitys(proList);

            clientId = R2 * (int) (R1 * Math.random());
            out.println("Client Connected:" + clientId);
            System.out.println("Client <" + clientId + "> Connected\n");

            while (true) {
                downloadFile();

                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        if (msg.contains("answer:")) {
                            msg = msg.substring(7);
                            int ans = Integer.parseInt(msg);
                            System.out.println("Client <" + clientId + ">: got reply " + msg + " for query " + query);
                            break;
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        private void downloadFile() {
            query = getNum(numsArr);
            String str = "Client <" + clientId + ">: sending " + query;
            System.out.println(str);
            out.println("sending:" + query);
        }
    }

    /**
     * gets R1,R2- the range of the numbers on file, the probabilities of the
     * numbers and put them in probabilities ArrayList.
     *
     * @param fileName the probability file name
     */
    public void readFile(int fileName) {
        try {
            FileReader fr = new FileReader(fileName + ".txt");
            BufferedReader br = new BufferedReader(fr);
            String str = "";
            char c;

            while ((c = (char) br.read()) != ',') {
                str = str + c;
            }
            R1 = Integer.parseInt(str);
            str = "";
            while ((c = (char) br.read()) != ',') {
                str = str + c;
            }
            R2 = Integer.parseInt(str);
            int word = 0;
            while (word < R2 - R1 + 1) {
                str = "";
                while ((c = (char) br.read()) != ',' && (int) c != 65535) {
                    str = str + c;
                }
                word++;
                double tmp = Double.parseDouble(str);
                proList.add(tmp);
            }
            br.close();
            fr.close();
        } catch (IOException ex) {
            System.out.print("Error reading file\n" + ex);
            System.exit(2);
        }
    }

    /**
     *
     * @param proList the probabilities AraayList
     * @return 1000 length Array with the numbers, every number gets array cells
     * respectively his probability to be chosen from 1000 times
     */
    public int[] probabilitys(ArrayList<Double> proList) {
        int range = 1000;
        int proArr[] = new int[range];
        double prob;
        int numOfTimes;
        int index, count = 0, nums = 0;

        while (nums <= R2 - R1) {
            prob = proList.get(nums);
            numOfTimes = (int) (prob * range);

            for (index = count; index < (count + numOfTimes); index++) {
                proArr[index] = R1 + nums;
            }
            nums++;
            count = index;
        }
        return proArr;
    }

    public int getNum(int[] proArr) {
        int index = (int) (Math.random() * 1000);

        return proArr[index];
    }

    public static void main(String[] args) {
        int fileName = Integer.parseInt(args[0]);

        Client client = new Client(fileName);
    }

}
