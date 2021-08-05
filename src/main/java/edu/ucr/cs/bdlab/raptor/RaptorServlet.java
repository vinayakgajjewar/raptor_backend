//package org.bdlabucr;
package edu.ucr.cs.bdlab.raptor;


import java.io.IOException;

// import java.util.*; // maps
import java.util.List; // lists

// for some reason, including this import causes the servlet to break :(
//import java.nio.file.Paths; // paths

// jackson library to read/write json files
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucr.cs.bdlab.beast.io.GeoJSONFeatureWriter;
import edu.ucr.cs.bdlab.beast.io.SpatialReader;
import edu.ucr.cs.bdlab.beast.JavaSpatialSparkContext;
import edu.ucr.cs.bdlab.beast.common.BeastOptions;
import edu.ucr.cs.bdlab.beast.geolite.IFeature;

import org.apache.spark.api.java.JavaRDD;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.api.java.JavaSparkContext;
// https://spark.apache.org/docs/latest/rdd-programming-guide.html
import org.apache.spark.SparkContext;
import org.apache.spark.SparkConf;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RaptorServlet extends HttpServlet {

    protected SparkConf sparkconf;
    protected JavaSparkContext sc;

    protected DBRead dbr;

    public RaptorServlet() {
        System.out.println("----initializing servlet");

        // initialize DB reader
        dbr = new DBRead();

        // create our JavaSparkContext
        // local[*] tells Spark to run with as many worker threads as there are cores on the machine
        sparkconf = new SparkConf().setAppName("appName").setMaster("local[*]");
        sc = new JavaSparkContext(sparkconf);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        dbr.read();

        // we set content-type as application/geo+json
        // not application/json
        response.setContentType("application/geo+json");
        response.setStatus(HttpServletResponse.SC_OK);

        // read an example geojson file into a list
        //long cnt = SpatialReader.readInput(sc, new BeastOptions(), "exampleinput.geojson", "geojson").count();
        //System.out.println(cnt);

        List<IFeature> records = SpatialReader.readInput(sc, new BeastOptions(), "state.geojson", "geojson").collect();
        //JavaRDD<IFeature> records = SpatialReader.readInput(sc, new BeastOptions(), "exampleinput.geojson", "geojson");

        System.out.println("----done reading records");

        // try writing out a record
        try (GeoJSONFeatureWriter writer = new GeoJSONFeatureWriter()) {
            writer.initialize(response.getOutputStream(), new Configuration());
            for (int i = 0; i < records.size(); i++) {
                writer.write(records.get(i));
            }
            //writer.write(records.get(0));
            //writer.write(records.first());
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}