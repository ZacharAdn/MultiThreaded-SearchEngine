import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

class clientInfo {

    private Socket socket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private int clientId;

    public clientInfo(Socket socket, BufferedReader in, PrintWriter out, int clientId) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.clientId = clientId;
    }

    public Task checkRequest() throws IOException {//the check request thread call this function
        String msg;
        if (in.ready()) {//check if in the BufferedReader massage to download
            if ((msg = in.readLine()).contains("sending:")) {
                int x = Integer.parseInt(msg.substring(8));
                Task item = new Task(x, clientId);
                return item;
            } else {
                return null;
            }
        }
        return null;
    }

    void ansToClient(Task ans) {
        out.println("answer:" + ans.getY());
    }

    public int getClientId() {
        return clientId;
    }

}
