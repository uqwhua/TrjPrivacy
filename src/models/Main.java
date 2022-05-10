package models;

import models.dummy.Dummy;
import models.glove.Glove;
import models.klt.KLT;
import models.mixzone.MixZone;
import shared.FileOutput;
import spatial.ComplexPoint;
import spatial.Grid;
import spatial.Trajectory;
import shared.FileInput;
import shared.Utils;

import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import static shared.FileInput.formalizeName;
import static shared.FileInput.getInputFilename;
import static shared.FileOutput.getOutputPrefix;

public class Main {

    public static void main(String[] args) throws IOException {

        System.out.println("------------------------------------------------------------------------------------------------------");
        System.out.println("|\t\t Trajectory Privacy Survey Project                                                           |");
        System.out.println("|\t\t Title:   A Survey and Experimental Study on Privacy-Preserving Trajectory Data Publishing   |");
        System.out.println("|\t\t Authors: Fengmei Jin, Wen Hua, Matteo Francia, Pingfu Chao, Maria E Orlowska, Xiaofang Zhou |");
        System.out.println("------------------------------------------------------------------------------------------------------");

        // parameters read from file "config.properties"
        Properties props = Utils.springUtil();
        final String model = props.getProperty("privacyModel");
        final String dataset = props.getProperty("dataset");
        final String variant = props.getProperty("variant");        // only for T-Drive

        final String inputFolder = props.getProperty("dataInput");
        final String inputFilename = getInputFilename(inputFolder, dataset, variant);
        final String outputFolder = props.getProperty("dataOutput") + formalizeName(model) + "/";

        final String roadNetworkFilename = props.getProperty("roadNetworkFile");
        final String poiFilename = props.getProperty("poiFile");
        final boolean outputResults = props.getProperty("outputResults").equals("true");

        final int k_anonymity = Integer.parseInt(props.getProperty("k-anonymity"));
        final int l_diversity = Integer.parseInt(props.getProperty("l-diversity"));
        final float t_closeness = Float.parseFloat(props.getProperty("t-closeness"));
        final float ratio_EL = Float.parseFloat(props.getProperty("exposure_ratio"));
        final float radius = Float.parseFloat(props.getProperty("radius"));
        final int min_trj_length = Integer.parseInt(props.getProperty("min_length"));
        final int max_mz = Integer.parseInt(props.getProperty("num_mixzones"));
        final float step = 0.001f; // to build grid (the paper used base stations as area)

        if(model.equalsIgnoreCase("glove") || model.equalsIgnoreCase("klt")) {
            ComplexPoint.needDelta = true;   // the initialization of deltaSecond and deltaLongitude, deltaLatitude
        }

        /* ------------------------------------- */
        /* -- Step-1: Read trajectories files -- */
        Vector<Trajectory> trajectories = new Vector<>();
        {
            System.out.println("[PROGRESS] Reading trajectory data from " + inputFilename);
            double avgLength = FileInput.readTrajectory(inputFilename, trajectories);
            System.out.printf("[INFO] average length of %d trajectories = %.3f\n\n", trajectories.size(), avgLength);
        }

        // ------- these two models need the POI and category information, build a grid index ahead
        Grid grid = null;
        if(model.equalsIgnoreCase("dummy") || model.equalsIgnoreCase("klt")) {
            System.out.println("[PROGRESS] build grid based on Beijing POIs ...");
            grid = FileInput.readBeijingPOIs(poiFilename, step, step);
        }

        /* ------- start anonymization ------- */
        String outputFilePrefix = getOutputPrefix(dataset, variant);
        Vector<Trajectory> outputTrajectories = new Vector<>();
        if(model.equalsIgnoreCase("mixzone")) {
            outputFilePrefix += radius + "_" + max_mz;
            MixZone.execute(trajectories, radius, max_mz, min_trj_length, step, step, roadNetworkFilename, outputTrajectories);
        }
        else if (model.equalsIgnoreCase("dummy")) {
            String filename = outputResults ? outputFolder + outputFilePrefix + k_anonymity + "_" + l_diversity + ".csv" : "";
            Dummy.execute(trajectories, grid, ratio_EL, k_anonymity, l_diversity, min_trj_length, outputTrajectories, filename);
            // NOTE: write the anonymized trajectories into a file in a batch manner
        }
        else {
            // ------- the Glove and KLT should compute the matrix before anonymization, otherwise too time-consuming
            if (model.equalsIgnoreCase("glove")) {
                outputFilePrefix += k_anonymity + "";
                Glove.execute(trajectories, k_anonymity, outputTrajectories);
            }
            else if (model.equalsIgnoreCase("klt")) {
                outputFilePrefix += k_anonymity + "_" + l_diversity + "_" + t_closeness;
                KLT.execute(trajectories, grid, k_anonymity, l_diversity, t_closeness, outputTrajectories);                // grid contains the POI and category information
            }
            else {
                System.out.println("[ERROR] incorrect privacy model given. \n\t Options: MixZone, Dummy, GLOVE, KLT.");
            }
        }

        // output the anonymized trajectories to file (except Dummy)
        if(outputResults && !model.equalsIgnoreCase("dummy")) {
            String filename = outputFolder + outputFilePrefix + ".csv";
            FileOutput.outputAnonymization(model, filename, outputTrajectories);
        }

        System.out.println("\n------------------------------------------------------------------------------------------------------");
        System.out.println("\nThe program is done!");
    }
}
