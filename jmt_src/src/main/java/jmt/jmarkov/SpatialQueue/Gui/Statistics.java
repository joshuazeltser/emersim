package jmt.jmarkov.SpatialQueue.Gui;

import jmt.jmarkov.Graphics.QueueDrawer;
import jmt.jmarkov.Graphics.constants.DrawNormal;
import jmt.jmarkov.Queues.Exceptions.NonErgodicException;
import jmt.jmarkov.Queues.MM1Logic;
import jmt.jmarkov.SpatialQueue.Simulation.SpatialQueueSimulator;
import jmt.jmarkov.utils.Formatter;

import javax.swing.*;
import java.awt.*;

import static jmt.jmarkov.SpatialQueue.Gui.GuiComponents.*;

/**
 * Created by joshuazeltser on 02/11/2016.
 */
public class Statistics {

    private static double  T;

    double U; // Utilization [%]
    double Q; // Average customer in station
    private String nStrS = "Avg. Cust. in Station (Queue + Service) N = ";
    private String nStrE = " cust.";
    private String uStrS = "Avg. Utilization (Sum of All Servers) U = ";
    private String uStrE = "";
    private String thrStrS = "Avg. Throughput X =";
    private String thrStrE = " cust./s";
    private String respStrS = "Avg. Response Time R = ";
    private String respStrE = " s";

    private boolean nonErgodic = false;//if the utilization is less than 1
    private MM1Logic ql;
    private QueueDrawer queueDrawer;
    private DrawNormal dCst;
    private JLabel mediaJobsL;
    private JLabel utilizationL;
    private JLabel thrL;
    private JLabel responseL;
    private double R;

    public Statistics() {
        ql = new MM1Logic(0, 0);
        queueDrawer = new QueueDrawer(ql, true);
        init();
        dCst = new DrawNormal();
    }

    //initialise components
    public void init() {
        mediaJobsL = new JLabel();
        utilizationL = new JLabel();
        thrL = new JLabel();
        responseL = new JLabel();
    }

    // update values for stats in the stats results at the bottom of the gui
    protected void updateFields(SpatialQueueSimulator sim) {
        try {
            Q = ql.mediaJobs();
            U = ql.utilization();
            utilizationL.setForeground(Color.BLACK);
            utilizationL.setText(uStrS + Formatter.formatNumber(U, 3) + uStrE);
            mediaJobsL.setText(nStrS + Formatter.formatNumber(Q, 3) + nStrE);

            T = ql.throughput();
            R = ql.responseTime();

            thrL.setText(thrStrS + Formatter.formatNumber(T, 3) + thrStrE);
            responseL.setText(respStrS + Formatter.formatNumber(R, 3) + respStrE);
            nonErgodic = false;

            if (sim != null && ql.getLambda() > 0) {
                sim.setLambdaZero(false);
            }
        } catch (NonErgodicException e) {
            // if saturation has been reached
            Q = 0.0;
            U = 0.0;
            mediaJobsL.setText(nStrS + "Saturation");

            utilizationL.setForeground(Color.RED);
            utilizationL.setText(uStrS + "Saturation");
            responseL.setText(respStrS + "Saturation");
            nonErgodic = true;
        }
        queueDrawer.setMediaJobs(Q - U);
    }

    // generate the initial stats for when the frame is first opened
    protected void generateSimulationStats(JPanel resultsP) {
        // media
        mediaJobsL.setText(nStrS + "0" + nStrE);
        mediaJobsL.setFont(dCst.getNormalGUIFont());
        resultsP.add(mediaJobsL);

        // utilization
        utilizationL.setText(uStrS + "0" + uStrE);
        utilizationL.setFont(dCst.getNormalGUIFont());
        resultsP.add(utilizationL);

        // throughput
        thrL.setText(thrStrS + "0" + thrStrE);
        thrL.setFont(dCst.getNormalGUIFont());
        resultsP.add(thrL);

        // response time
        responseL.setText(respStrS + "0" + respStrE);
        responseL.setFont(dCst.getNormalGUIFont());
        resultsP.add(responseL);
    }

    //setup queue visualisation and pointer
    protected void showQueue() {

        ql = new MM1Logic(0, 0);

        queueDrawer.updateLogic(ql);
        queueDrawer.setMaxJobs(0);
        queueDrawer.setCpuNumber(1);
        updateFields(sim);
    }

    // set the service time
    public void setSI(double sI) {
        ql.setS(sI / 1000);
        updateFields(sim);

    }

    public void setLambda(double lambda) {
        ql.setLambda(lambda);
        updateFields(sim);
    }

    public QueueDrawer getQueueDrawer() {
        return queueDrawer;
    }

    public MM1Logic getQueueLogic() {
        return ql;
    }


}