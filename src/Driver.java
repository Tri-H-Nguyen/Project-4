import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.openstreetmap.gui.jmapviewer.*;

import java.awt.*;
import java.awt.event.*;


public class Driver {
	
	// Declare class data
	private static final int zoomLevel = 6;
	
	private static int time;
	private static boolean stopsEnabled;
	private static boolean readyToPlay;
	private static boolean currentlyRunning;

	private static JFrame frame;
	private static JMapViewer mapViewer;
	

    public static void main(String[] args) throws FileNotFoundException, IOException {

    	// Read file and call stop detection
    	TripPoint.readFile("triplog.csv");
    	TripPoint.h2StopDetection();
    	
    	// Set up frame, include your name in the title
    	frame = new JFrame("Project 4 - Tri Nguyen");
    	frame.setSize(1000,600);
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.setLayout(new BorderLayout());
        
        // Set up Panel for input selections
    	JPanel inputPanel = new JPanel();
    	inputPanel.setLayout(new FlowLayout());
        
    	
        // Play Button
    	JButton playButton = new JButton("Play");
    	currentlyRunning = false;
         
    	
        // CheckBox to enable/disable stops
    	JCheckBox stopCheckBox = new JCheckBox();
    	stopCheckBox.setSelected(false);
    	stopsEnabled = false;
        
    	
        // ComboBox to pick animation time
    	JComboBox<String> timeComboBox = new JComboBox<>();
    	timeComboBox.addItem("Animation Time");
    	timeComboBox.addItem("15");
    	timeComboBox.addItem("30");
    	timeComboBox.addItem("60");
    	timeComboBox.addItem("90");
    	readyToPlay = false;
        
    	
        // Add all to top panel
    	inputPanel.add(timeComboBox);
    	inputPanel.add(stopCheckBox);
    	inputPanel.add(new JLabel("Include Stops"));
    	inputPanel.add(playButton);
    	frame.add(inputPanel, BorderLayout.NORTH);
        
        
        // Set up mapViewer
    	System.setProperty("http.agent", "Project4-TripViewer/1.0");
    	mapViewer = new JMapViewer();
    	
        
        
        // Add listeners for GUI components
        playButton.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		System.out.println(readyToPlay);
        		if (readyToPlay && !currentlyRunning) {
        			try {
						animate();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
        		}
        	}
        });
        
        stopCheckBox.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if (stopCheckBox.isSelected()) {
        			stopsEnabled = true;
        		} else {
        			stopsEnabled = false;
        		}
        		System.out.println(stopsEnabled);
        	}
        });
        
        timeComboBox.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		try {
	        		time = Integer.parseInt((String) timeComboBox.getSelectedItem());
	        		readyToPlay = true;
	        		System.out.println(time);
        		} catch (Exception ex) {
        			readyToPlay = false;
        		}
        	}
        });

        // Set the map center and zoom level
        mapViewer.setDisplayPosition(center(), zoomLevel);
        frame.add(mapViewer, BorderLayout.CENTER);
        
        frame.setVisible(true);
    }
    
    public static Coordinate center() {
    	double lowestLat = 0;
    	double highestLat = 0;
    	double lowestLon = 0;
    	double highestLon = 0;
    	
    	for (TripPoint t : TripPoint.getTrip()) {
    		if (t.equals(TripPoint.getTrip().get(0))) {
    			lowestLat = t.getLat(); highestLat = t.getLat();
    			lowestLon = t.getLon(); highestLon = t.getLon();
    			continue;
    		}
    		
    		if (t.getLat() < lowestLat) {
    			lowestLat = t.getLat();
    		} else if (t.getLat() > highestLat) {
    			highestLat = t.getLat();
    		}
    		
    		if (t.getLon() < lowestLon) {
    			lowestLon = t.getLon();
    		} else if (t.getLon() > highestLon) {
    			highestLon = t.getLon();
    		}
    	}
    	return new Coordinate((lowestLat + highestLat) / 2, (lowestLon + highestLon) / 2);
    }

    
    // Animate the trip based on selections from the GUI components
    public static void animate() throws IOException {
    	currentlyRunning = true;
    	mapViewer.removeAllMapMarkers();
    	mapViewer.removeAllMapPolygons();
    	ArrayList<TripPoint> whereToGo;
    	if (stopsEnabled) {
    		whereToGo = TripPoint.getTrip();
    	} else {
    		whereToGo = TripPoint.getMovingTrip();
    	}
    	
    	final Image raccoonImage = ImageIO.read(new File("raccoon.png"));
    	IconMarker raccoonIcon = new IconMarker(new Coordinate(whereToGo.get(0).getLat(),whereToGo.get(0).getLon()), 1, raccoonImage);
    	mapViewer.addMapMarker(raccoonIcon);
    	
    	int inBetweenTime = ((time - 1) * 1000000) / whereToGo.size();
    	System.out.println(inBetweenTime);
    	
    	long start = System.nanoTime();

    	new Thread(() -> {
    		for (int i = 0; i < whereToGo.size() - 1; i++) {
	    		try {
					TimeUnit.MICROSECONDS.sleep(inBetweenTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		Coordinate coord1 = new Coordinate(whereToGo.get(i).getLat(), whereToGo.get(i).getLon());
	    		Coordinate coord2 = new Coordinate(whereToGo.get(i + 1).getLat(), whereToGo.get(i + 1).getLon());
	    		
	    		SwingUtilities.invokeLater(() -> {
		    		mapViewer.addMapPolygon(new MapPolygonImpl(new ArrayList<>(Arrays.asList(coord1, coord2, coord2))));
		    		mapViewer.removeAllMapMarkers();
		    		mapViewer.addMapMarker(new IconMarker(coord2, raccoonImage));
	    		});	
	    		if (i == whereToGo.size() - 2) {
	    			currentlyRunning = false;
	    			System.out.println("Done: " + ((double) (System.nanoTime() - start) / 1000000000));
	    		}
	    	}
    	}).start();
    	
    	System.out.println(whereToGo.size());
    }
}