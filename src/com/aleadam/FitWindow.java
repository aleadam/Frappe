package com.aleadam;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import flanagan.analysis.Regression;
import ij.measure.ResultsTable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

public class FitWindow {
	private JFrame frame;
	private JLabel topLabel;
	private JPanel centerPanel;
	private int model;

	private String[] labels;
	private double[] time;
	private double[][] results;
	private double[] averages, devs, estimates;
	private String parameters;

	private String path;

	private static DecimalFormat df = new DecimalFormat ("0.####");

	private static int SINGLE_EXP = 1;
	private static int DOUBLE_EXP = 2;

	private void createWindow (String expPath, String windowTitle) {
		frame = new JFrame (windowTitle);
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		Container pane = frame.getContentPane();
		pane.setLayout (new BorderLayout());
		JPanel topPanel = new JPanel();
		topPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		topLabel = new JLabel ();
		topPanel.add(topLabel);
		centerPanel = new JPanel();
		centerPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		pane.add(topPanel, BorderLayout.NORTH);
		pane.add(centerPanel, BorderLayout.CENTER);
		JPanel bottomPanel = new JPanel();
		JButton saveButton = new JButton ("Save data");
		saveButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				int saveStatus = Frappe.OK;
				do {
					saveStatus = saveDataTable ();
				} while (saveStatus == Frappe.ERROR);
			}

		});
		bottomPanel.add(saveButton);
		pane.add(bottomPanel, BorderLayout.SOUTH);

		path = expPath;
	}

	private FitWindow (int type, String expPath, int expts, Regression reg) {
		model = type;
		if (type == SINGLE_EXP) {
			createWindow (expPath, "Single exponential Fit");
			double[] coeff= reg.getBestEstimates();
			double[] errors = reg.getBestEstimatesErrors();

			String str = new String ("Curve fitting results for " + expts + " FRAP curves");
			topLabel.setText (str);
			parameters = str;

			str = new String ("Single exponential model");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			centerPanel.add(new JSeparator ());

			str = new String ("Mobile Fraction: " + df.format(100*coeff[0]) + 
					" +- " + df.format(100*errors[0]) + 
					" (" + df.format(100*coeff[0]-100*errors[0]) + 
					" - " + df.format(100*coeff[0]+100*errors[0]) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String ("t1/2: " + df.format(-Math.log(0.5)/coeff[1]) + 
					" (" + df.format(-Math.log(0.5)/(coeff[1]+errors[1])) + 
					" - " + df.format(-Math.log(0.5)/(coeff[1]-errors[1])) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String ("R2: " + df.format(reg.getCoefficientOfDetermination()));
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String ("Adj R2: " + df.format(reg.getAdjustedCoefficientOfDetermination()));
			centerPanel.add(new JLabel (str));
			parameters += "," + str;			

		} else if (type == DOUBLE_EXP) {
			createWindow (expPath, "Double exponential Fit");
			double[] coeff= reg.getBestEstimates();
			double[] errors = reg.getBestEstimatesErrors();

			String str = new String ("Curve fitting results for " + expts + " FRAP curves");
			topLabel.setText (str);
			parameters = str;

			str = new String ("Double exponential model");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			centerPanel.add(new JSeparator ());

			str = new String  ("Mobile Fraction 1, " + df.format(100*coeff[0]) + 
					" +- " + df.format(100*errors[0]) + 
					" (" + df.format(100*coeff[0]-100*errors[0]) + 
					" - " + df.format(100*coeff[0]+100*errors[0]) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String  ("Mobile Fraction 2, " + df.format(100*coeff[1]) + 
					" +- " + df.format(100*errors[1]) + 
					" (" + df.format(100*coeff[1]-100*errors[1]) + 
					" - " + df.format(100*coeff[1]+100*errors[1]) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String  ("t1/2 1, " + df.format(-Math.log(0.5)/coeff[2]) +
					" (" + df.format(-Math.log(0.5)/(coeff[2]+errors[2])) + 
					" - " + df.format(-Math.log(0.5)/(coeff[2]-errors[2])) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String  ("t1/2 2, " + df.format(-Math.log(0.5)/coeff[3]) + 
					" (" + df.format(-Math.log(0.5)/(coeff[3]+errors[3])) + 
					" - " + df.format(-Math.log(0.5)/(coeff[3]-errors[3])) + ")");
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String  ("R2: " + df.format(reg.getCoefficientOfDetermination()));
			centerPanel.add(new JLabel (str));
			parameters += "," + str;

			str = new String  ("Adj R2: " + df.format(reg.getAdjustedCoefficientOfDetermination()));
			centerPanel.add(new JLabel (str));
			parameters += "," + str;
		}
	}

	public static FitWindow createSingleExpResultsWindow (String expPath, int expts, Regression regSE) {
		return new FitWindow (SINGLE_EXP, expPath, expts, regSE);
	}

	public static FitWindow createDoubleExpResultsWindow (String expPath, int expts, Regression regSE) {
		return new FitWindow (DOUBLE_EXP, expPath, expts, regSE);
	}

	public void addDataTable (String[] dataLabels, double[] dataTime, double[][] dataTable, double[] dataAverage, double[] dataDevs, double[] dataEstimates) {
		labels = dataLabels;
		time = dataTime;
		results = dataTable;
		averages = dataAverage;
		devs = dataDevs;
		estimates = dataEstimates;
	}

	public void show () {
		frame.pack();
		frame.setVisible(true);
	}

	private int saveDataTable () {
		String fname = path;
		File f = new File (fname);
		boolean overWrite = false;
		while (f.exists() && !overWrite) {
			JFileChooser chooser = new JFileChooser(fname);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
			chooser.setFileFilter(filter);
			int val = chooser.showSaveDialog(frame);
			if (val != JFileChooser.APPROVE_OPTION) {
				return Frappe.CANCEL;
			}
			f = chooser.getSelectedFile();
			fname = f.getPath();
			if (f.exists()) {
				int n = JOptionPane.showConfirmDialog(frame, "File exists. Do you want to overwrite it?", null, JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.YES_OPTION)
					overWrite = true;
			}
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));

			String str[] = parameters.split(",");
			for (String s : str) {
				bw.write(s);
				bw.newLine();
			}
			bw.newLine();
			bw.newLine();
			bw.write("Data table:");
			bw.newLine();

			for (int i=0; i<labels.length-1; i++) {
				bw.write(labels[i] + ",");
			}
			bw.write(labels[labels.length-1]);
			bw.newLine();

			int totalFrames = results[0].length;
			int expts = results.length;
			for (int j=0; j<totalFrames; j++) {
				bw.write(time[j] + ",");
				for (int i=0; i<expts; i++) {
					bw.write (results[i][j] + ", ");
				}
				bw.write (averages[j] + "," + devs[j] + "," + estimates[j]);
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			int n = JOptionPane.showConfirmDialog(frame, "There was an error writing the file. Retry?", null, JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.YES_OPTION)
				return Frappe.ERROR;
			else
				return Frappe.CANCEL;
		}
		return Frappe.OK;
	}

}
