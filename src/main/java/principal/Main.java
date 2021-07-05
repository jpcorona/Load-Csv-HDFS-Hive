package principal;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Main{

    public static void main(String[] args) {
        try {

            long startTime = System.currentTimeMillis();

            // validate args
            validateArgs(args);
            String fileOportunidadPath = args[0];
            String fileTenenciaPath = args[1];
            String fileTransaccionPath = args[2];
            String fileJourneyPath = args[3];

            // create hive connection
            Connection hiveConnection = createDBConnection();
            Statement statement = hiveConnection.createStatement();
            System.out.println("Connection created");

            Configuration conf = new Configuration();
            FileSystem fs = FileSystem.get(conf);

            // save Oportunidad
            String oportunidadDestination = String.format("%s_opportunity_copy.csv", fileOportunidadPath.split(".csv")[0]);
            Path inFile = new Path(fileOportunidadPath);
            Path outFile = new Path(oportunidadDestination);
            FSDataInputStream inOportunidad = fs.open(inFile);
            FSDataOutputStream outOportunidad = fs.create(outFile);
            saveData(inOportunidad, outOportunidad, statement, "cdltmp.mc_logs_track_opp", oportunidadDestination);


            // save tenencia
            String tenenciaDestination = String.format("%s_tenencia_copy.csv", fileTenenciaPath.split(".csv")[0]);
            Path inFileTentencia = new Path(fileTenenciaPath);
            Path outFileTentencia = new Path(tenenciaDestination);
            FSDataInputStream inTenencia = fs.open(inFileTentencia);
            FSDataOutputStream outTenencia = fs.create(outFileTentencia);
            saveData(inTenencia, outTenencia, statement, "cdltmp.mc_logs_track_tna", tenenciaDestination);


            // save transaccion
            String transaccionDestination = String.format("%s_transaccion_copy.csv", fileTransaccionPath.split(".csv")[0]);
            Path inFileTransaccion = new Path(fileTransaccionPath);
            Path outFileTransaccion = new Path(transaccionDestination);
            FSDataInputStream inTransaccion = fs.open(inFileTransaccion);
            FSDataOutputStream outTransaccion = fs.create(outFileTransaccion);
            saveData(inTransaccion, outTransaccion, statement, "cdltmp.mc_logs_track_trn", transaccionDestination);

            // save journey
            String journeyDestination = String.format("%s_journey_copy.csv", fileJourneyPath.split(".csv")[0]);
            Path inFileJourney = new Path(fileJourneyPath);
            Path outFileJourney = new Path(journeyDestination);
            FSDataInputStream inJorunery = fs.open(inFileJourney);
            FSDataOutputStream outJourney = fs.create(outFileJourney);
            saveData(inJorunery, outJourney, statement, "journeyactivity", journeyDestination);


            hiveConnection.close();

            long endTime = System.currentTimeMillis();
            System.out.println("Time final" + (endTime - startTime) + " milliseconds");

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static void validateArgs(String[] args) throws Exception {
        if (args.length < 4) {
            String message = "Must be sent file oportunidad, tenencia, transaccion and journey path";
            throw new Exception(message);
        }
    }

    private static void saveData(FSDataInputStream in, FSDataOutputStream out, Statement statement, String tableName, String destinationPath) throws SQLException, IOException {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        String datePart = ft.format(date);
        Integer lineNumber = 0;
        String line;
        while ((line = in.readLine()) != null) {
            if(lineNumber == 0) {
                lineNumber+=1;
                continue;
            }
            if(line.trim().isEmpty()) {
                continue;
            }
            System.out.println("Tabla: "+ tableName + " line: "+line);
            out.writeBytes(line);
            out.writeBytes("\n");
        }
        in.close();
        out.close();
        String query = String.format("LOAD DATA INPATH '%s' OVERWRITE INTO TABLE %s PARTITION(data_date_part=%s)", destinationPath, tableName, datePart);
        System.out.println("Execute load file query: " + query);
        statement.execute(query);
    }

    //Hive
    private static Connection createDBConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.cloudera.hive.jdbc4.HS2Driver");
        return DriverManager.getConnection("jdbc:hive2://dmtrbg001.dcorporativo.cl.corp:10000/cdltmp;AuthMech=1;KrbHostFQDN=dmtrbg001.dcorporativo.cl.corp;KrbServiceName=hive;KrbRealm=DCORPORATIVO.CL.CORP");
    }

}

