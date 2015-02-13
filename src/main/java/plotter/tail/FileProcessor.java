package plotter.tail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Reads data from a file and plots it.
 * Even though a {@link DataFile} will only have one current FileProcessor,
 * multiple may be running.
 * This is because there isn't a reliable way to interrupt a thread blocking in
 * {@link BufferedReader#readLine()}, so to restart reading a file,
 * we abandon the current FileProcessor and start a new one.
 * @author Adam Crume
 */
class FileProcessor implements Runnable {
    /** The plot. */
    private final TailPlot tailPlot;

    /** The file we are processing. */
    private DataFile dataFile;

    /** If true, abort processing.  Synchronize on this. */
    private boolean stop;

    /** Reads the file. */
    private BufferedReader in;


    /**
     * Creates a file processor.
     * @param tailPlot the plot
     * @param dataFile file to process
     */
    public FileProcessor(TailPlot tailPlot, DataFile dataFile) {
        this.tailPlot = tailPlot;
        this.dataFile = dataFile;
    }


    /**
     * Stops the processor.
     * Note that it may not stop immediately.
     */
    public void stop() {
        synchronized(this) {
            stop = true;
            try {
                if(in != null) {
                    in.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void run() {
        File file = dataFile.getFile();
        if(file == null) {
            in = new BufferedReader(new InputStreamReader(System.in));
        } else {
            try {
                in = new BufferedReader(new FileReader(file));
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            dataFile.clearData();
            tailPlot.resetMinMax();

            int lineNumber = 0;
            // Shuffles data from the IO thread to the GUI thread.
            final List<double[]> buffer = new ArrayList<double[]>();
            long oldFileSize = 0;
            while(true) {
                if(file != null && tailPlot.isAutorestart()) {
                    long fileSize = file.length();
                    if(fileSize < oldFileSize) {
                        dataFile.restart();
                        return;
                    }
                    oldFileSize = fileSize;
                }
                synchronized(this) {
                    if(stop) {
                        return;
                    }
                }
                String line = in.readLine();
                if(line == null) {
                    if(file == null) {
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                    }
                    continue;
                }
                lineNumber++;

                final double[] ddata = dataFile.processLine(lineNumber, line);

                if(ddata == null) {
                    continue;
                }

                synchronized(buffer) {
                    boolean empty = buffer.isEmpty();
                    buffer.add(ddata);
                    if(empty) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                synchronized(buffer) {
                                    MetaAxis metaX = tailPlot.getMetaX();
                                    boolean isXLogscale = metaX.isLogscale();
                                    for(double[] ddata : buffer) {
                                        double xVal = ddata[0];
                                        if(isXLogscale) {
                                            xVal = Math.log10(xVal);
                                        }
                                        boolean anyVisible = false;
                                        for(int i = 1; i < ddata.length; i++) {
                                            double val = ddata[i];
                                            Field field = dataFile.getFields().get(i - 1);
                                            boolean visible = field.isVisible();
                                            MetaAxis fieldY;
                                            if(field.isOnY2()) {
                                                fieldY = tailPlot.getMetaY2();
                                            } else {
                                                fieldY = tailPlot.getMetaY();
                                            }
                                            if(fieldY.isLogscale()) {
                                                val = Math.log10(val);
                                            }
                                            if(visible) {
                                                fieldY.updateMinMax(val);
                                                anyVisible = true;
                                            }
                                            field.getDataset().add(xVal, val);
                                        }
                                        if(anyVisible) {
                                            metaX.updateMinMax(xVal);
                                        }
                                    }
                                    buffer.clear();
                                }
                                tailPlot.commitMinMax();
                            }
                        });
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
