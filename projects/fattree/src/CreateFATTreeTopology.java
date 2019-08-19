/**
 * Created by slin on 4/7/17.
 */
import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CreateFATTreeTopology {

    private static FileWriter[] coreSwitches;

    private static FileWriter[][] aggSwitches;

    private static FileWriter[][] edgeSwitches;

    private static String CONFIG_FILES_DIR;

    private static int k;

    private static int K_OVER_2;

    private enum Type {
        CORE, AGGREGATE, EDGE
    }

    private enum Protocol {
        OSPF, BGP
    }

    public static void main(String[] args) {
        Protocol protocol = null;
        if (args.length < 6 || args.length > 6) {
            System.err.println("Error: missing or additional arguments");
            System.err.println(String.format("Usage: java %1$s -k <number of ports (must be an even integer)> -p <protocol: ospf or bgp> -dir <output directory the configs will be written to>",
                    CreateFATTreeTopology.class.getSimpleName()));
            System.exit(1);
        } else {
            k = Integer.parseInt(args[1]);
            protocol = args[3].equals("ospf") ? Protocol.OSPF : Protocol.BGP;
            CONFIG_FILES_DIR = args[5]+'/';
        }

        File dir = new File(CONFIG_FILES_DIR);
        if (dir.listFiles() == null) {
            System.out.println(CONFIG_FILES_DIR + " doesn't exist.");
            System.exit(1);
        }

        for (File file : dir.listFiles()) { // deleting all files in CONFIG_FILES_DIR because FileWriters need append mode
            file.delete();
        }

        K_OVER_2 = k >> 1;
        coreSwitches = new FileWriter[K_OVER_2*K_OVER_2];
        aggSwitches = new FileWriter[k][K_OVER_2];
        edgeSwitches = new FileWriter[k][K_OVER_2];

        for (Type type : Type.values()) {
            try {
                generateConfigFileWriters(type);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }

        }
        try {
            createFATTreeTopology(protocol);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param type - the switch type
     * Given type, generate cfg FileWriters and store them into a corresponding 1D/2D array
     * fileName: 'ci.cfg'   ,'c' stands for core, 'i', index
     *           'ai-j.cfg' ,'a' stands for aggregate, 'i', pod index, 'j', switch index within pod
     *           'ei-j.cfg' ,'e' stands for edge, 'i', pod index, 'j', switch index within pod
     */
    private static void generateConfigFileWriters(Type type) throws IOException {
        FileWriter fileWriter = null;
        if (type.equals(Type.CORE)) {
            for (int i = 0; i < coreSwitches.length; i++) {
                fileWriter = new FileWriter(CONFIG_FILES_DIR+"c"+i+".cfg", true);
                coreSwitches[i] = fileWriter;
            }
        } else {
            String filename = type == Type.AGGREGATE ? "agg" : "e";
            FileWriter[][] fileWriters = type == Type.AGGREGATE ? aggSwitches : edgeSwitches;
            for (int i = 0; i < k; i++) { // loop over each pod
                for (int j = 0; j < K_OVER_2; j++) {
                    String name = filename+i+"-"+j;
                    fileWriter = new FileWriter(CONFIG_FILES_DIR+name+".cfg", true);
                    fileWriters[i][j] = fileWriter;
                }
            }
        }
    }

    /**
     * Create links between core and agg switches, and between agg and edge switches
     *
     * Given a pod index (podIdx), switch index (aggSwitchIdx) and port index (aggPortIdx) of an agg switch, to find the core switch and port
     *  that links with the agg switch's port, we use the formula:
     *                      coreSwitchIdx = aggSwitchIdx * k/2 + aggPortIdx
     * Idea: we can partition (k/2)^2 core switches into (k/2) groups of (k/2) core switches
     *      - aggSwitchIdx: helps us find the right group of core switches
     *      - aggPortIdx: helps us find the right core switch within the group
     *
     * Which portIdx on the core switch we should link to is determined by podIdx (from an agg switch's point of view).
     *
     * Creating links between agg and edge layers is more straightforward
     *
     * For a given aggSwitch, the first available port is [k/2 + aggPortIdx] because we used the other k/2 to connect with core layer
     * aggPortIdx determines which edge switch with which current agg switch's port is to be linked
     *
     * Special Indexing is used to ensure that, for example, pod 0 has hosts from 0 to (k/2)^2 -1
     *                                                       pod 1 has hosts from (k/2)^2 to 2*(k/2)^2 -1, etc.
     *
     * Assigning IP address for an interface on a switch/router: [IPv4 addressing]
     *  IP address is in the format: byte1.byte2.byte3.byte4
     *
     *  For interfaces between:         core and agg layers      |      agg and edge        |       edge and host
     *      byte1(IpLayer) =                    10               |          20              |           30
     *                                                           |                          |
     *      byte2          =               coreSwitchIdx         |        podIdx            |           podIdx
     *                                                           |                          |
     *      byte3          =                  podIdx             |      aggSwitchIdx        |           aggPortIdx (edgeSwitchIdx)
     *                                                           |                          |
     *      byte4          =                0(core)/1(agg)       |  aggPortIdx*2/(+1)       |         aggSwitchIdx*2 (*2 because our subnet mask is /31)
     *
     */
    private static void createFATTreeTopology(Protocol protocol) throws IOException{
        Boolean nonOSPF = protocol.equals(protocol.BGP) ? true : false;

        BufferedWriter[] coreSwitchBWs = new BufferedWriter[coreSwitches.length];
        for (int i= 0; i < coreSwitches.length; i++) { // constructs BufferedWriters for core switches
            coreSwitchBWs[i] = new BufferedWriter(coreSwitches[i]);
            coreSwitchBWs[i].write("!\nversion 12.4\n!\nhostname "+"c"+i+"\n");
        }

        StringBuilder[] coreSwitchBGPNeighborNetworkSBs = protocol.equals(Protocol.BGP) ? new StringBuilder[coreSwitches.length] : null;

        BufferedWriter[] edgeSwtichBWs = new BufferedWriter[K_OVER_2];

        for (int podIdx= 0; podIdx < k; podIdx++) { // loop over each pod

            for (int j= 0; j < K_OVER_2; j++) { // constructs BufferedWriters for edge switches
                edgeSwtichBWs[j] = new BufferedWriter(edgeSwitches[podIdx][j]);
                edgeSwtichBWs[j].write("!\nversion 12.4\n!\nhostname e"+podIdx+"-"+j+"\n");
            }

            StringBuilder[] edgeSwitchBGPNeighborNetworkSBs = protocol.equals(Protocol.BGP) ? new StringBuilder[edgeSwitches.length] : null;

            for (int aggSwitchIdx= 0; aggSwitchIdx < K_OVER_2; aggSwitchIdx++) { // loop over each agg switch
                BufferedWriter aggSwitchBW =
                        new BufferedWriter(aggSwitches[podIdx][aggSwitchIdx]); // constructs BufferedWriter for current agg switch
                // try-with resources statement, so no closing necessary
                aggSwitchBW.write("!\nversion 12.4\n!\nhostname agg"+podIdx+"-"+aggSwitchIdx+"\n");

                StringBuilder aggSwitchBGPNeighborNetworkSB = protocol.equals(Protocol.BGP) ? new StringBuilder() : null;

                for (int aggPortIdx= 0; aggPortIdx < K_OVER_2; aggPortIdx++) {

                    // creating links between CORE and AGGREGATE layers
                    int coreSwitchIdx = aggSwitchIdx*K_OVER_2 + aggPortIdx; // core switch index that maps to current port of current agg switch
                    BufferedWriter coreSwitchBW = coreSwitchBWs[coreSwitchIdx];

                    coreSwitchBW.write(generateIfaceConfigString(10, coreSwitchIdx, podIdx,
                            0, podIdx+1,nonOSPF));
                    aggSwitchBW.write(generateIfaceConfigString(10, coreSwitchIdx,
                            podIdx, 1, aggPortIdx+1,nonOSPF));

                    // creating links between AGGREGATE and EDGE layers
                    BufferedWriter edgeSwitchBW = edgeSwtichBWs[aggPortIdx]; // aggPortIdx is the same as Idx used
                    // to find the corresponding edge switch
                    int byte4 = aggPortIdx*2;
                    aggSwitchBW.write(generateIfaceConfigString(20, podIdx, aggSwitchIdx,
                            byte4, aggPortIdx+1+K_OVER_2,nonOSPF));
                    edgeSwitchBW.write(generateIfaceConfigString(20, podIdx, aggSwitchIdx,
                            byte4+1, aggSwitchIdx+1, nonOSPF));

                    // creating links between EDGE layer and host
                    edgeSwitchBW.write(generateIfaceConfigString(30, podIdx, aggPortIdx,
                            aggSwitchIdx*2, aggSwitchIdx+1+K_OVER_2,true));

                    // if routers are running eBGP protocol
                    if (protocol.equals(Protocol.BGP)) {
                        // between CORE and AGGREGATE layers
                        StringBuilder coreSwitchBGPNeighborNetworkSB = coreSwitchBGPNeighborNetworkSBs[coreSwitchIdx];
                        if (coreSwitchBGPNeighborNetworkSB == null) {
                            coreSwitchBGPNeighborNetworkSBs[coreSwitchIdx] = new StringBuilder();
                        }

                        generateBGPConfigForNeighborNetwork(coreSwitchBGPNeighborNetworkSBs[coreSwitchIdx],
                                10, coreSwitchIdx, podIdx, 0, 1);
                        generateBGPConfigForNeighborNetwork(aggSwitchBGPNeighborNetworkSB,
                                10, coreSwitchIdx, podIdx, 1, 0);

                        // between AGGREGATE and EDGE layers
                        generateBGPConfigForNeighborNetwork(aggSwitchBGPNeighborNetworkSB,
                                20, podIdx, aggSwitchIdx, byte4, byte4+1);

                        StringBuilder edgeSwitchBGPNeighborNetworkSB = edgeSwitchBGPNeighborNetworkSBs[aggPortIdx];
                        if (edgeSwitchBGPNeighborNetworkSB == null) {
                            edgeSwitchBGPNeighborNetworkSBs[coreSwitchIdx] = new StringBuilder();
                        }
                        generateBGPConfigForNeighborNetwork(edgeSwitchBGPNeighborNetworkSBs[aggPortIdx],
                                20, podIdx, aggSwitchIdx, byte4+1, byte4);

                        // between EDGE and HOSTs
                        generateBGPConfigForNeighborNetwork(edgeSwitchBGPNeighborNetworkSBs[aggPortIdx],
                                30, podIdx, aggPortIdx, aggSwitchIdx*2, aggSwitchIdx*2+1);
                    }
                }

                if (protocol.equals(Protocol.BGP)) {
                    aggSwitchBW.write(generateBGPConfigString(aggSwitchBGPNeighborNetworkSB.toString(),
                            coreSwitches.length+1+aggSwitchIdx));
                } else {
                    aggSwitchBW.write(generateOSPFConfigString(1, new int[]{10, 20}, new int[]{0, 1}));
                }
                aggSwitchBW.write("!\nend\n");
                aggSwitchBW.close();
            }

            for (int j= 0; j < K_OVER_2; j++) { // closing BufferedWriters for edge switches
                if (protocol.equals(Protocol.BGP)) {
                    edgeSwtichBWs[j].write(generateBGPConfigString(edgeSwitchBGPNeighborNetworkSBs[j].toString(),
                            coreSwitches.length + aggSwitches.length + 1 + j));
                } else {
                    edgeSwtichBWs[j].write(generateOSPFConfigString(1, new int[]{20, 30}, new int[]{1, 2}));
                }
                edgeSwtichBWs[j].write("!\nend\n");
                edgeSwtichBWs[j].close();
            }
        }

        for (int i= 0; i < coreSwitches.length; i++) { // closing BufferedWriters for core switches
            if (protocol.equals(Protocol.BGP)) {
                coreSwitchBWs[i].write(generateBGPConfigString(coreSwitchBGPNeighborNetworkSBs[i].toString(), i+1));
            } else {
                coreSwitchBWs[i].write(generateOSPFConfigString(1, new int[]{10}, new int[]{0}));
            }
            coreSwitchBWs[i].write("!\nend\n");
            coreSwitchBWs[i].close();
        }
    }


    private static void generateBGPConfigForNeighborNetwork(StringBuilder sb, int byte1, int byte2, int byte3, int byte4, int neighByte4) {
        sb.append("\tneighbor ").append(byte1 +"."+ byte2 + "." + byte3 + "." + neighByte4 +"\n")
                .append("\tnetwork ").append(byte1 +"."+ byte2 + "." + byte3 + "." + byte4 + "/31\n");
    }

    private static String generateIfaceConfigString(int byte1, int byte2, int byte3, int byte4, int portIdx, boolean nonOSPF) {
        StringBuilder sb = new StringBuilder("!\n")
                .append("interface GigabitEthernet0/"+ portIdx)
                .append("\n\tip address "+ byte1 +"."+ byte2 + "." + byte3 + "." + byte4)
                .append(" 0.0.0.2\n");
        if (!nonOSPF) {
            sb.append("\tip ospf cost 1\n");
        }
        return sb.toString();
    }

    private static String generateOSPFConfigString(int processID, int[] IpLayers, int[] areaIDs) {
        StringBuilder sb = new StringBuilder("!\nrouter ospf ").append(processID)
                .append("\n\tredistribute connected\n");
        for (int i = 0; i < IpLayers.length; i++) {
            sb.append("\tnetwork " + IpLayers[i] + ".0.0.0 0.255.255.255 area " + areaIDs[i] +"\n");
        }
        return sb.toString();
    }

    private static String generateBGPConfigString(String bgpNeighborNetwork, int ASNnumber) {
        StringBuilder sb = new StringBuilder("!\nrouter bgp ").append(ASNnumber+"\n")
                .append(bgpNeighborNetwork);
        return sb.toString();
    }

}

