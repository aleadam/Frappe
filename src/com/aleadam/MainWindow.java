package com.aleadam;

import flanagan.analysis.Regression;
import ij.gui.Plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

import org.apache.commons.io.FilenameUtils;

public class MainWindow implements ActionListener {

	JFrame frame;
	JTabbedPane tabbedPane;
	JLabel status;

	public MainWindow () {

		frame = new JFrame ("Frappe");
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar (createMenuBar ());

		JPanel pane = (JPanel) frame.getContentPane();
		pane.setLayout (new BorderLayout());
		tabbedPane = new JTabbedPane ();
		tabbedPane.setPreferredSize(new Dimension (1024, 512));
		pane.add(tabbedPane, BorderLayout.CENTER);

		status = new JLabel ();
		pane.add(status, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}

	public JMenuBar createMenuBar() {
		JMenuBar menuBar;
		JMenu menu, submenu;
		JMenuItem menuItem;

		menuBar = new JMenuBar();
		menu = new JMenu ("File");
		menu.setMnemonic (KeyEvent.VK_F);
		menu.getAccessibleContext().setAccessibleDescription ("Load and save FRAP experiments");
		menuBar.add (menu);

		menuItem = new JMenuItem ("Open Experiment", KeyEvent.VK_O);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Open a complete FRAP experiment");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menuItem = new JMenuItem ("Save Experiment", KeyEvent.VK_S);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Save a complete FRAP experiment");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menu.addSeparator();

		menuItem = new JMenuItem ("Load ZVI file", KeyEvent.VK_L);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Load a single FRAP video");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menuItem = new JMenuItem ("Close ZVI file", KeyEvent.VK_C);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Close the current FRAP video");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menu.addSeparator();

		menuItem = new JMenuItem ("Load ROIs", KeyEvent.VK_R);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Load ROIs for the current FRAP video");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menuItem = new JMenuItem ("Save ROIs", KeyEvent.VK_T);
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_T, ActionEvent.CTRL_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription ("Save the ROIs for the current FRAP video");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menu.addSeparator();

		menuItem = new JMenuItem ("Exit", KeyEvent.VK_E);
		menuItem.getAccessibleContext().setAccessibleDescription ("Exit the application");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		menu = new JMenu ("Measure");
		menu.setMnemonic (KeyEvent.VK_M);
		menuBar.add (menu);

		submenu = new JMenu ("Get intensity values");
		menuItem = new JMenuItem ("For the current ROI");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_C, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menuItem = new JMenuItem ("For all ROIs");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_A, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menu.add (submenu);

		submenu = new JMenu ("Get normalized data");
		menuItem = new JMenuItem ("Double normalization");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_N, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menuItem = new JMenuItem ("Full scale");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_F, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menu.add (submenu);

		submenu = new JMenu ("Fit data");
		menuItem = new JMenuItem ("Single exponential");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_S, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menuItem = new JMenuItem("Double exponential");
		menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_D, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		submenu.add (menuItem);
		menu.add (submenu);

		menu.addSeparator();

		menuItem = new JMenuItem ("Use bleach frame for all images");
		menuItem.addActionListener(this);
		menu.add (menuItem);

		return menuBar;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JMenuItem source = (JMenuItem)(e.getSource());
		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() {
				boolean result = true;
				String s = source.getText();
				if (s.equals("Open Experiment"))
					result = loadExp ();
				if (s.equals("Save Experiment")) {
					int saveStatus = Frappe.OK;
					do {
						saveStatus = saveExp ();
					} while (saveStatus == Frappe.ERROR);
				}
				if (s.equals("Load ZVI file"))
					result = loadZvi ();
				if (s.equals("Close ZVI file")) {
					int i = tabbedPane.getSelectedIndex();
					tabbedPane.remove(i);
				}
				if (s.equals("Load ROIs"))
					result = loadRois ();
				if (s.equals("Save ROIs")) {
					int saveStatus = Frappe.OK;
					do {
						saveStatus = saveRois ();
					} while (saveStatus == Frappe.ERROR);
				}
				else if (s.equals ("Exit"))
					System.exit(0);

				else if (s.equals("For the current ROI"))
					result = getSingleData ();
				else if (s.equals("For all ROIs"))
					getAllData ();
				else if (s.equals("Double normalization"))
					result = getDoubleNorm ();
				else if (s.equals("Full scale"))
					result = getFullScale ();
				else if (s.equals("Single exponential"))
					result = fitCurveSingleExp ();
				else if (s.equals("Double exponential"))
					result = fitCurveDoubleExp ();
				else if (s.equals("Use bleach frame for all images")) {
					int bleachFrame = ((ExpFrame) tabbedPane.getSelectedComponent()).getBleachFrame();
					for (int i=0; i<tabbedPane.getComponentCount(); i++) {
						((ExpFrame) tabbedPane.getComponentAt(i)).setBleachFrame(bleachFrame);
					}
				}

				return result;
			}
			@Override
			protected void done() {
				try {
					if(this.get()) {
						status.setText("");
					} else {
						status.setText("An error has occured. Please see the log file.");
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		};
		sw.execute();
	}

	private boolean loadExp () {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("FRAP experiment files", "frappe");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if(returnVal != JFileChooser.APPROVE_OPTION) {
			return false;
		} 
		File file = chooser.getSelectedFile();

		tabbedPane.removeAll();
		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader (new FileReader(file));
			int expts = Integer.valueOf(bufRdr.readLine());
			for (int i=0; i<expts; i++) {
				File f = new File (bufRdr.readLine());
				if (f != null) {
					status.setText("Loading image: " + FilenameUtils.getBaseName(f.getPath()));
					ExpFrame imageFrame = (ExpFrame) (tabbedPane.add(FilenameUtils.getBaseName(f.getPath()), new ExpFrame(f)));
					imageFrame.setBleachFrame (Integer.valueOf(bufRdr.readLine()));
					String rois = bufRdr.readLine();
					if (rois != "NOROIS") {
						imageFrame.setRois(FrapExperiment.FRAP_ROI, rois);
						imageFrame.setRois(FrapExperiment.STD_ROI, bufRdr.readLine());
						imageFrame.setRois(FrapExperiment.BG_ROI, bufRdr.readLine());
					}
				}

			}
			return true;
		} catch (BadArgumentException | IOException | DependencyException | ServiceException | FormatException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int saveExp () {
		String path = FilenameUtils.getFullPath(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
		String fname = new String (path);
		File f = new File (fname);
		if (f.exists())
			fname = new String (path);
		boolean overWrite = false;
		while (f.exists() && !overWrite) {
			JFileChooser chooser = new JFileChooser(fname);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("FRAP experiment files", "frappe");
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

			bw.write(Integer.toString (tabbedPane.getComponentCount()));
			bw.newLine();
			for (int i=0; i<tabbedPane.getComponentCount(); i++) {
				String name = ((ExpFrame) (tabbedPane.getComponent(i))).getExpName();
				bw.write(name);
				bw.newLine();
				bw.write(Integer.toString (((ExpFrame) tabbedPane.getComponent(i)).getBleachFrame()));
				bw.newLine();
				String[] text = ((ExpFrame) tabbedPane.getComponent(i)).getRoiList();
				if (text == null) {
					bw.write("NOROIS");
				} else {
					for (String str : text) {
						bw.write (str);
						bw.newLine();
					}
				}
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

	private boolean loadZvi () {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Axiovision ZVI Images", "zvi");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if(returnVal != JFileChooser.APPROVE_OPTION) {
			return false;
		} 
		File[] files = chooser.getSelectedFiles();
		if (files.length == 0) return false;
		for (File f : files) {
			try {
				status.setText("Loading image: " + FilenameUtils.getBaseName(f.getPath()));
				tabbedPane.addTab(FilenameUtils.getBaseName(f.getPath()), new ExpFrame(f));
				loadRois ((ExpFrame) (tabbedPane.getComponentAt(tabbedPane.getComponentCount()-1)), f);
			} catch (DependencyException | ServiceException
					| FormatException | IOException e) {
				e.printStackTrace();
			}
		}
		if (tabbedPane.getComponentCount() > 0)
			return true;
		else
			return false;
	}

	private void loadRois (ExpFrame imageFrame, File f) {
		String path = FilenameUtils.getFullPath(f.getPath());
		String baseName = FilenameUtils.getBaseName(f.getPath());
		File file = new File (path + baseName + ".rois");
		if (!file.exists())
			return;

		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader (new FileReader(file));
			imageFrame.setRois(FrapExperiment.FRAP_ROI, bufRdr.readLine());
			imageFrame.setRois(FrapExperiment.STD_ROI, bufRdr.readLine());
			imageFrame.setRois(FrapExperiment.BG_ROI, bufRdr.readLine());
		} catch (BadArgumentException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	private boolean loadRois () {
		String path = FilenameUtils.getFullPath(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
		JFileChooser chooser = new JFileChooser(path);
		chooser.setMultiSelectionEnabled(false);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("ROIs files", "rois");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if(returnVal != JFileChooser.APPROVE_OPTION) {
			return false;
		} 
		File file = chooser.getSelectedFile();
		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader (new FileReader(file));
			ExpFrame imageFrame = ((ExpFrame) tabbedPane.getSelectedComponent());
			imageFrame.setRois(FrapExperiment.FRAP_ROI, bufRdr.readLine());
			imageFrame.setRois(FrapExperiment.STD_ROI, bufRdr.readLine());
			imageFrame.setRois(FrapExperiment.BG_ROI, bufRdr.readLine());
			return true;
		} catch (BadArgumentException | IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	private int saveRois () {
		String[] text = ((ExpFrame) tabbedPane.getSelectedComponent()).getRoiList();
		String path = FilenameUtils.getFullPath(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
		String baseName = FilenameUtils.getBaseName(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
		String fname = new String (path + baseName + ".rois");
		File f = new File (fname);
		if (f.exists())
			fname = new String (path);
		boolean overWrite = false;
		while (f.exists() && !overWrite) {
			JFileChooser chooser = new JFileChooser(fname);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("ROIs files", "rois");
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

			for (String str : text) {
				bw.write (str);
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

	private boolean getSingleData () {
		try {
			status.setText("Measuring ROI data");
			double[] singleData = ((ExpFrame) tabbedPane.getSelectedComponent()).getData();
			double[] timeStamps = ((ExpFrame) tabbedPane.getSelectedComponent()).getFrapExp().getTimeStamps();
			String name = FilenameUtils.getBaseName(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			String roiName = ((ExpFrame) tabbedPane.getSelectedComponent()).getCurrentRoi();
			Plot plot = new Plot("ROI data - " + roiName + " : " + name, "Time (s)", "Norm Int", timeStamps, singleData);
			plot.show();

			return true;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean getAllData () {
		try {
			status.setText("Measuring ROI data");
			double[][] data = ((ExpFrame) tabbedPane.getSelectedComponent()).getAllData();
			double[] timeStamps = ((ExpFrame) tabbedPane.getSelectedComponent()).getFrapExp().getTimeStamps();
			String name = FilenameUtils.getBaseName(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			for (int i=0; i<data.length; i++) {
				String roiName = "";
				switch (i) {
				case 0:
					roiName = "FRAP ROI";
					break;
				case 1:
					roiName = "Standard ROI";
					break;
				case 2:
					roiName = "Background";
					break;
				case 3:
					roiName = "Total Intensity";
				}
				Plot plot = new Plot("ROI data - " + roiName + " : " + name, "Time (s)", "Norm Int", timeStamps, data[i]);
				plot.show();
			}

			return true;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean getDoubleNorm () {
		try {
			status.setText("Normalizing ROI data");
			ExpFrame imageFrame = ((ExpFrame) tabbedPane.getSelectedComponent());
			double[] data = ((ExpFrame) tabbedPane.getSelectedComponent()).doubleNormalization();
			int bleachFrame = imageFrame.getBleachFrame ();
			double[] timeStamps = imageFrame.getFrapExp().getTimeStamps();
			int totalFrames = imageFrame.getNFrames() - bleachFrame;
			double[] time = new double [totalFrames];
			double[] normalizedData = new double [totalFrames];
			double totalTime = timeStamps [timeStamps.length-1] - timeStamps[bleachFrame];
			for (int t=0; t<totalFrames; t++) {
				time[t] = totalTime * t / totalFrames;
				normalizedData[t] = data[t+bleachFrame];
			}
			String name = FilenameUtils.getBaseName(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			Plot plot = new Plot("Double normalization : " + name, "Time (s)", "Norm Int", time, normalizedData);
			plot.show();

			return true;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean getFullScale () {
		try {
			status.setText("Normalizing ROI data");
			ExpFrame imageFrame = ((ExpFrame) tabbedPane.getSelectedComponent());
			double[] data = imageFrame.fullScale();
			int bleachFrame = imageFrame.getBleachFrame ();
			double[] timeStamps = imageFrame.getFrapExp().getTimeStamps();
			int totalFrames = imageFrame.getNFrames() - bleachFrame;
			double[] time = new double [totalFrames];
			double[] normalizedData = new double [totalFrames];
			double totalTime = timeStamps [timeStamps.length-1] - timeStamps[bleachFrame];
			for (int t=0; t<totalFrames; t++) {
				time[t] = totalTime * t / totalFrames;
				normalizedData[t] = data[t+bleachFrame];
			}
			String name = FilenameUtils.getBaseName(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			Plot plot = new Plot("Full scale normalization : " + name, "Time (s)", "Norm Int", time, normalizedData);
			plot.show();

			return true;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean fitCurveSingleExp () {
		try {
			int expts = tabbedPane.getComponentCount();
			int bleachFrame = ((ExpFrame) tabbedPane.getSelectedComponent()).getBleachFrame ();
			double[] timeStamps = ((ExpFrame) tabbedPane.getSelectedComponent()).getFrapExp().getTimeStamps();

			int length = Integer.MAX_VALUE;
			for (int e=0; e<expts; e++) {
				int size = ((ExpFrame) tabbedPane.getComponentAt(e)).getNFrames();
				length = length < size ? length : size;
			}
			int totalFrames = length - bleachFrame;
			double[] time = new double [totalFrames];
			double totalTime = timeStamps [timeStamps.length-1] - timeStamps[bleachFrame];
			for (int t=0; t<totalFrames; t++) {
				time[t] = totalTime * t / totalFrames;
			}

			double[][][] data = new double [expts][][];
			double[][] norm = new double [expts][];

			for (int e=0; e<expts; e++) {
				status.setText("Measuring ROI data for image: " + ((ExpFrame) tabbedPane.getComponentAt(e)).getExpName());
				data[e] =  ((ExpFrame) tabbedPane.getComponentAt(e)).getAllData();
				status.setText("Normalizing ROI data for image: " + ((ExpFrame) tabbedPane.getComponentAt(e)).getExpName());
				norm[e] = ((ExpFrame) tabbedPane.getComponentAt(e)).fullScale ();
			}

			double[][] normalizedData = new double [expts][totalFrames];
			for (int e=0; e<expts; e++) {
				for (int t=0; t<totalFrames; t++) {
					normalizedData[e][t] = norm[e][t+bleachFrame];
				}
			}

			double[] averages = new double [totalFrames];
			double[] devs = new double [totalFrames];
			for (int t=0; t<totalFrames; t++) {
				double average = 0;
				int count = 0;
				for (int e=0; e<expts; e++) {
					count++;
					average += normalizedData[e][t];
				}
				average /= count;

				double stDev = 0;
				for (int i=0; i<expts; i++) {
					if (t < length) {
						stDev += Math.pow(normalizedData[i][t] - average, 2) / (count - 1);
					}
				}
				stDev = Math.sqrt(stDev);
				averages[t] = average;
				devs[t] = stDev;
			}

			status.setText("Fitting ROI data");

			FunctionFullScaleSingleExp fse = new FunctionFullScaleSingleExp ();
			Regression regSE = new Regression(time, averages, devs);      

			double[] startS = new double[2];
			startS[0] = 0.5;
			startS[1] = 0.05;
			double[] stepS = new double[2];
			stepS[0] = 0.1;
			stepS[1] = 0.01;

			regSE.addConstraint(0, -1, 0);
			regSE.addConstraint(0, 1, 1);
			regSE.supressPrint();
			regSE.simplex(fse, startS, stepS);
			double[] estimates = regSE.getYcalc();

			String path = FilenameUtils.getFullPath(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			String[] labels = new String[expts+4];
			labels[0] = "Time (s)";
			for (int i=0; i<expts; i++) {
				labels[i+1] = ((ExpFrame) tabbedPane.getComponent(i)).getExpName();
			}
			labels[expts+1] = "Average Normalized Intensity";
			labels[expts+2] = "Standard Deviation";
			labels[expts+3] = "Calculated Values (Double exp)";
			
			FitWindow fw = FitWindow.createSingleExpResultsWindow(path, expts, regSE);
			fw.addDataTable(labels, time, normalizedData, averages, devs, estimates);
			fw.show();
			return true;

		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean fitCurveDoubleExp () {
		try {
			status.setText("Fitting ROI data");
			int expts = tabbedPane.getComponentCount();
			int bleachFrame = ((ExpFrame) tabbedPane.getSelectedComponent()).getBleachFrame ();
			double[] timeStamps = ((ExpFrame) tabbedPane.getSelectedComponent()).getFrapExp().getTimeStamps();

			int length = Integer.MAX_VALUE;
			for (int e=0; e<expts; e++) {
				int size = ((ExpFrame) tabbedPane.getComponentAt(e)).getNFrames();
				length = length < size ? length : size;
			}
			int totalFrames = length - bleachFrame;
			double[] time = new double [totalFrames];
			double totalTime = timeStamps [timeStamps.length-1] - timeStamps[bleachFrame];
			for (int t=0; t<totalFrames; t++) {
				time[t] = totalTime * t / totalFrames;
			}


			double[][][] data = new double [expts][][];
			double[][] norm = new double [expts][];

			for (int e=0; e<expts; e++) {
				status.setText("Measuring ROI data for image: " + ((ExpFrame) tabbedPane.getComponentAt(e)).getExpName());
				data[e] =  ((ExpFrame) tabbedPane.getComponentAt(e)).getAllData();
				status.setText("Normalizing ROI data for image: " + ((ExpFrame) tabbedPane.getComponentAt(e)).getExpName());
				norm[e] = ((ExpFrame) tabbedPane.getComponentAt(e)).fullScale ();
			}

			double[][] normalizedData = new double [expts][totalFrames];
			for (int e=0; e<expts; e++) {
				for (int t=0; t<totalFrames; t++) {
					normalizedData[e][t] = norm[e][t+bleachFrame];
				}
			}

			double[] averages = new double [totalFrames];
			double[] devs = new double [totalFrames];
			for (int t=0; t<totalFrames; t++) {
				double average = 0;
				int count = 0;
				for (int e=0; e<expts; e++) {
					count++;
					average += normalizedData[e][t];
				}
				average /= count;

				double stDev = 0;
				for (int i=0; i<expts; i++) {
					if (t < length) {
						stDev += Math.pow(normalizedData[i][t] - average, 2) / (count - 1);
					}
				}
				stDev = Math.sqrt(stDev);
				averages[t] = average;
				devs[t] = stDev;
			}

			status.setText("Fitting ROI data");

			FunctionFullScaleDoubleExp fde = new FunctionFullScaleDoubleExp (); 
			Regression regDE = new Regression(time, averages, devs);      

			double[] startD = new double[4];
			startD[0] = 0.5;
			startD[1] = 0.5;
			startD[2] = 0.05;
			startD[3] = 0.05;
			double[] stepD = new double[4];
			stepD[0] = 0.1;
			stepD[1] = 0.1;
			stepD[2] = 0.01;
			stepD[3] = 0.01;

			int[] ind = {0, 1}; 
			int[] pom = {1, 1}; 
			regDE.addConstraint(ind, pom, 1, 1);
			regDE.addConstraint(0, -1, 0);
			regDE.addConstraint(1, -1, 0);
			regDE.supressPrint();
			regDE.simplex(fde, startD, stepD);
			double[] estimates = regDE.getYcalc();

			String path = FilenameUtils.getFullPath(((ExpFrame) tabbedPane.getSelectedComponent()).getExpName());
			String[] labels = new String[expts+4];
			labels[0] = "Time (s)";
			for (int i=0; i<expts; i++) {
				labels[i+1] = ((ExpFrame) tabbedPane.getComponent(i)).getExpName();
			}
			labels[expts+1] = "Average Normalized Intensity";
			labels[expts+2] = "Standard Deviation";
			labels[expts+3] = "Calculated Values (Double exp)";
			
			FitWindow fw = FitWindow.createDoubleExpResultsWindow(path, expts, regDE);
			fw.addDataTable(labels, time, normalizedData, averages, devs, estimates);
			fw.show();
			return true;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}
}

