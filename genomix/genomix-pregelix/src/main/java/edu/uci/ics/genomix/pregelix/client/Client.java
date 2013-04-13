
package edu.uci.ics.genomix.pregelix.client;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.uci.ics.pregelix.api.job.PregelixJob;
import edu.uci.ics.pregelix.core.base.IDriver.Plan;
import edu.uci.ics.pregelix.core.driver.Driver;

public class Client {

    private static class Options {
        @Option(name = "-inputpaths", usage = "comma seprated input paths", required = true)
        public String inputPaths;

        @Option(name = "-outputpath", usage = "output path", required = true)
        public String outputPath;

        @Option(name = "-ip", usage = "ip address of cluster controller", required = true)
        public String ipAddress;

        @Option(name = "-port", usage = "port of cluster controller", required = false)
        public int port;

        @Option(name = "-plan", usage = "query plan choice", required = false)
        public Plan planChoice = Plan.OUTER_JOIN;

        @Option(name = "-runtime-profiling", usage = "whether to do runtime profifling", required = false)
        public String profiling = "false";
    }

    public static void run(String[] args, PregelixJob job) throws Exception {
        Options options = prepareJob(args, job);
        Driver driver = new Driver(Client.class);
        driver.runJob(job, options.planChoice, options.ipAddress, options.port, Boolean.parseBoolean(options.profiling));
    }

    private static Options prepareJob(String[] args, PregelixJob job) throws CmdLineException, IOException {
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        parser.parseArgument(args);

        String[] inputs = options.inputPaths.split(";");
        FileInputFormat.setInputPaths(job, inputs[0]);
        for (int i = 1; i < inputs.length; i++)
            FileInputFormat.addInputPaths(job, inputs[0]);
        FileOutputFormat.setOutputPath(job, new Path(options.outputPath));
        return options;
    }

}