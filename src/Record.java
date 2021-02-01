import java.sql.Timestamp;
import java.util.Date;

public class Record {

    private String name;
    private boolean state;
    private Timestamp time;
    private String IP;
    private int port;

    public Record(String name, boolean state, String IP, int port) {
        this.name = name;
        this.state = state;
        Date date = new Date();
        this.time = new Timestamp(date.getTime());
        this.port = port;
        this.IP = IP;
    }

    @Override
    public String toString() {
        String temp;
        if (state) {
            temp = "Joined";
        }else
            temp = "left";
        return "Record{" +
                "name='" + name + '\'' +
                ", state=" + temp +
                ", time=" + time +
                ", IP='" + IP + '\'' +
                ", port=" + port +
                '}';
    }
}
