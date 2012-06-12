package pipeline;

import gui.PipelineApp;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import util.ElapsedTimeFormatter;
import util.LoggingOutputStream;
import util.OperatorTimeSummary;

public class Pipeline {

	protected File source;
	protected Document xmlDoc;
	public static final String PYTHON_SCRIPTS_DIR="python.scripts.dir";
	public static final String PROJECT_HOME="home";
	public static final String START_TIME="start.time";
	public static final String END_TIME="end.time";
	public static final String primaryLoggerName = "pipeline.primary";
	protected Logger primaryLogger = Logger.getLogger(primaryLoggerName);
	protected String defaultLogFilename = "pipelinelog";
	protected ObjectHandler handler = null;
	
	
	//Default number of threads to use
	protected int threadCount = 12;
	
	
	//Right now DEBUG just emits all log messages to std out
	public static final boolean DEBUG = false;
	
	//Stores some basic properties, such as paths to commonly used executables
	protected Properties props;
	public static final String defaultPropertiesPath = ".pipelineprops.xml";
	private String propertiesPath = defaultPropertiesPath;
	//public static Pipeline pipelineInstance;
	private Date startTime = null;
	
	public Pipeline(File inputFile) {
		this(inputFile, null);
	}
	
		
	public Pipeline(File inputFile, String propsPath) {
		this.source = inputFile;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(source);
		} catch (ParserConfigurationException e) {
			System.out.println("ParserConfigException : " + e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("SAXException : " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException : " + e.getMessage());
			e.printStackTrace();
		}
		
		if (propsPath != null)
			setPropertiesPath(propsPath);
		initializeLogger();
		loadProperties();	
	}
	
	/**
	 * Create a new Pipeline object that will attempt to execute the Document provided
	 * @param doc
	 */
	public Pipeline(Document doc) {
		this.source = null;
		xmlDoc = doc;
		initializeLogger();
		loadProperties();	
	}
	
	/**
	 * Static getter for main application object
	 * @return
	 */
//	public static Pipeline getPipelineInstance() {
//		return pipelineInstance;
//	}
	
	/**
	 * Get preferred size of thread pools
	 * @return
	 */
	public int getThreadCount() {
		return threadCount;
	}
	
	/**
	 * Get the input file from which the pipeline was 
	 * @return
	 */
	public File getSourceFile() {
		return source;
	}
	
	/**
	 * Obtain the value of the property associated with the given key, or null
	 * if no such property exists or if the properties were not successfully loaded
	 * (for instance, if no properties file was found)
	 * @param key
	 * @return Value associated with key provided
	 */
//	public static Object getPropertyStatic(String key) {
//		return pipelineInstance.getProperty(key);
//	}
	
	/**
	 * Returns the value of the PROJECT_HOME property
	 * @return
	 */
	public String getProjectHome() {
		return (String)this.getProperty(PROJECT_HOME);
	}
	
	/**
	 * Obtain a property from this pipeline object, or null if the property has not been set
	 * @param key
	 * @return
	 */
	public Object getProperty(String key) {
		if (props != null)
			return props.get(key);
		else
			return null;
	}
	
	/**
	 * Returns all keys associated with Properties for this pipeline instance
	 * @return
	 */
	public Collection<Object> getPropertyKeys() {
		if (props == null)
			return new ArrayList<Object>();
		else
			return props.keySet();
	}
	/**
	 * Add a property to this pipeline object
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		props.setProperty(key, value);
	}
	
	/**
	 * Set the location of the properties file to use. This only has has effect when called
	 * prior to loadProperties()
	 * @param pathToPropsFile
	 */
	public void setPropertiesPath(String pathToPropsFile) {
		System.out.println("Setting properties path to : " + pathToPropsFile);
		propertiesPath = pathToPropsFile;
	}
	
	/**
	 * Attempt to load some basic properties from a persistent file
	 */
	private void loadProperties() {
		//First check to see if properties file is in user dir, if so use it
		File propsFile = null;
		if (! propertiesPath.equals(defaultPropertiesPath)) { //User has set properties path to something special
			propsFile = new File(propertiesPath);
		}
		else { //User has not set properties path, use defaults...
			String userDir = System.getProperty("user.dir");
			propsFile = new File(userDir + "/" + defaultPropertiesPath);

			//If its not in user.dir, then check home directory, if not there then abort
			if (! propsFile.exists()) {
				String homeDir = System.getProperty("user.home");
				propsFile = new File(homeDir + "/" + defaultPropertiesPath);
				
			}
		}
		
		//Can't find properties file
		if (! propsFile.exists()) {
			primaryLogger.warning("Could not find default properties file, no file at path " + propsFile.getAbsolutePath());
			return;
		}

		primaryLogger.info("Loading properties from " + propsFile.getAbsolutePath());
		props = new Properties();
		try {
			FileInputStream propStream = new FileInputStream(propsFile);
			props.loadFromXML(propStream);
			
			StringBuffer msg = new StringBuffer("Loaded following properties: \n");
			
			for(Object key : props.keySet()) {
				msg.append(key.toString() + " = " + props.getProperty(key.toString()) + "\n");
			}
			primaryLogger.info(msg.toString());
		} catch (FileNotFoundException e) {
			primaryLogger.warning("Could not open stream for properties file at path " + propsFile.getAbsolutePath());
		} catch (InvalidPropertiesFormatException e) {
			primaryLogger.warning("Could not read from default properties file: \n" + e.getCause() + "\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			primaryLogger.warning("Could not read from default properties file: \n" + e.getCause() + "\n" + e.getLocalizedMessage());
		}
		
		//Parse thread pool size from properties
		String threadAttr = props.getProperty(PipelineXMLConstants.THREADS_ATTR);
		if (threadAttr != null) {
			int threads = Integer.parseInt( threadAttr );
			this.threadCount = threads;
			primaryLogger.info("Setting default thread count to : " + threadCount);
		}
		
		//Set the PROJECT_HOME property to user.dir, unless it was already specified
		if (props.getProperty(PROJECT_HOME) == null) {
			String pHome = System.getProperty("user.dir");
			if (! pHome.endsWith("/")) {
				pHome = pHome + "/";
			}
			primaryLogger.info("Setting PROJECT_HOME to " + pHome);
			props.setProperty(PROJECT_HOME, pHome);
		}
	}

	/**
	 * Perform a bit of initialization for the main Logger
	 */
	private void initializeLogger() {

		if (DEBUG) {
			primaryLogger.addHandler(new Handler() {
				@Override
				public void publish(LogRecord record) {
					System.out.println(record.getMessage());
				}

				@Override
				public void flush() {
					System.out.flush();
				}
				public void close() throws SecurityException { }
			});
		}
		
//		try {
//			FileHandler logHandler = new FileHandler(defaultLogFilename + ".xml", true); //Append to existing log
//			primaryLogger.addHandler( logHandler );
//	
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
//			e.printStackTrace();
//		} catch (IOException e) {
//			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
//			e.printStackTrace();
//		}
		

		Date beginTime = new Date();
		primaryLogger.info("\n\n***************************************************************** \n " + beginTime + " Beginning new Pipeline run");
	}
	
	
	
	/**
	 * Attempt to read, parse, and create the objects as specified in the document
	 * @throws PipelineDocException
	 * @throws ObjectCreationException
	 */
	public void initializePipeline() throws PipelineDocException, ObjectCreationException {
		
		//See if we should capture stderr and redirect it to the log file
		if (props.getProperty(PipelineXMLConstants.CAPTURE_ERR) != null) {
			System.out.println("Got property for capture err: " + props.getProperty(PipelineXMLConstants.CAPTURE_ERR));
			Boolean capture = Boolean.parseBoolean( props.getProperty(PipelineXMLConstants.CAPTURE_ERR));
			if (capture) {
				Logger stdErrLog = Logger.getLogger("StdErr");
				try {
					String projHome = this.getProjectHome();
					String errorLogPath = projHome + "/error_log.xml";
					System.out.println("Writing error log to path: " + errorLogPath);

					FileHandler stdErrHandler = new FileHandler(errorLogPath, false);
					stdErrHandler.setFormatter(new java.util.logging.SimpleFormatter());
					stdErrLog.addHandler(stdErrHandler);
					final LoggingOutputStream logStream = new LoggingOutputStream(stdErrLog, Level.WARNING);
					final PrintStream errStream = new PrintStream(logStream, true);

					System.setErr(errStream);
					System.err.println("Here's a test string from std err...");
					primaryLogger.info("Directing all output from standard error to std. error logger at path: " + errorLogPath);
				} catch (SecurityException e) {
					primaryLogger.warning("Could not open file error_log.xml  : " + e.getMessage());
					e.printStackTrace();
				} catch (IOException e) {
					primaryLogger.warning("Could not open file error_log.xml: " + e.getMessage());
					e.printStackTrace();
				} 

			}
		}
				
		if (props.getProperty(PipelineXMLConstants.MAIL_RECIPIENT) != null) {
			String mailRecipient = props.getProperty(PipelineXMLConstants.MAIL_RECIPIENT);
			
		}
		
		if (xmlDoc == null) {
			primaryLogger.severe(" ERROR : XML document not found / defined, aborting run ");
			throw new PipelineDocException("XMLDoc not defined");
		}
		
		Element docElement = xmlDoc.getDocumentElement();
		String docRootName = docElement.getNodeName();
		if (! docRootName.equals(PipelineXMLConstants.DOCUMENT_ROOT)) {
			throw new PipelineDocException("Document root name should be " + PipelineXMLConstants.DOCUMENT_ROOT + ", but found : " + docRootName);
		}
		
		
		//Add in a little gizmo to track how long we spend on each operation
		OperatorTimeSummary profiler = new OperatorTimeSummary();
		this.addListener(profiler);
		
		
		primaryLogger.info("XML Document found and parsed, attempting to read objects");
				
		handler = new ObjectHandler(this, xmlDoc);
		
		//Set the project home field
		String projHome = props.getProperty(PROJECT_HOME);
		if (projHome != null && projHome.length()>0 && (!projHome.equals(System.getProperty("user.dir")))) {
			
			try {
				if (!projHome.endsWith("/"))
					projHome = projHome + "/";
				Date now = new Date();
				Calendar cal = Calendar.getInstance();
				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH);
				int day = cal.get(Calendar.DATE);
				int hour = cal.get(Calendar.HOUR);
				int min = cal.get(Calendar.MINUTE);
				
				String suffix = "" + day + month + year + "-" + hour + "-" + min;
				
				FileHandler fileHandler = new FileHandler(projHome + "pipeinstancelog-" + suffix + ".xml");
				primaryLogger.addHandler(fileHandler);
			} catch (SecurityException e) {
				primaryLogger.warning("Could not create handler for proj-home specific log file, reason: " + e.getLocalizedMessage());
			} catch (IOException e) {
				primaryLogger.warning("Could not create handler for proj-home specific log file, reason: " + e.getLocalizedMessage());
			}
			
			
		}

	
		
		
		//A quick scan for errors / validity would be a good idea
		
		fireMessage("Reading objects");
		handler.readObjects();
		System.err.flush(); //Make sure info is written to logger if necessary
		int opCount = handler.getOperatorList().size();
		primaryLogger.info("Successfully read objects, found " + opCount + " operators ... pipeline is now initialized");
		fireMessage("Pipeline initialized");
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	/**
	 * Attempt to run the pipeline defined in the source input file
	 * @throws PipelineDocException If there are errors in document structure
	 * @throws ObjectCreationException If errors arise regarding instantiation of particular objects
	 * @throws OperationFailedException 
	 */
	public void execute() throws OperationFailedException {
		startTime = new Date();
		
		primaryLogger.info("Executing pipeline");
		
		for(Operator op : handler.getOperatorList()) {
			try {
				Date opStart = new Date();
				fireOperatorBeginning(op);
				op.setAttribute(START_TIME, "" + opStart.getTime());
				primaryLogger.info("Executing operator : " + op.getObjectLabel() + " class: " + op.getClass());
				op.operate();
				System.err.flush(); //Make sure info is written to logger if necessary
				Date end = new Date();
				primaryLogger.info("Operator : " + op.getObjectLabel() + " class: " + op.getClass() + " has completed, operator elapsed time: " + ElapsedTimeFormatter.getElapsedTime(opStart.getTime(), end.getTime()) + "\n Pipeline elapsed time: " + ElapsedTimeFormatter.getElapsedTime(startTime.getTime(), end.getTime()));
				fireOperatorCompleted(op);
				op.setAttribute(END_TIME, "" + end.getTime());
			} catch (OperationFailedException e) {
				fireMessage("Operator failed : " + e);
				e.printStackTrace();
				primaryLogger.severe("ERROR : Operator : " + op.getObjectLabel() + " (class " + op.getClass() + ") failed \n Cause : " + e.getMessage());
				
				//We want to throw it again so other objects will be notified of this event besides
				//through the weak 'fireMessage' avenue
				throw e;
			}
		}
		
		firePipelineFinished();
		long endTime = System.currentTimeMillis();
		
		primaryLogger.info("Finished executing all operators, pipeline is done. \n Total elapsed time " + ElapsedTimeFormatter.getElapsedTime(startTime.getTime(), endTime ));
	}
	
	
	/**
	 * Add a new listener to be notified of various pipeline events
	 * @param l
	 */
	public void addListener(PipelineListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}
	
	/**
	 * Remove the given listener from the list of listeners
	 * @param l
	 */
	public void removeListener(PipelineListener l) {
		listeners.remove(l);
	}
	
	/**
	 * Notify all listeners that the given operator has completed its job
	 * @param op
	 */
	public void fireOperatorCompleted(Operator op) {
		for(PipelineListener listener : listeners) {
			listener.operatorCompleted(op);
		}
	}
	
	/**
	 * Notify all listeners that the given operator has begun to work
	 * @param op
	 */
	public void fireOperatorBeginning(Operator op) {
		for(PipelineListener listener : listeners) {
			listener.operatorBeginning(op);
		}
	}
	
	/**
	 * Notify all listeners that all operators have completed and the pipeline has finished
	 * @param op
	 */
	public void firePipelineFinished() {
		for(PipelineListener listener : listeners) {
			listener.pipelineFinished();
		}
	}
	
	/**
	 * Send a text message to all listeners
	 * @param message
	 */
	public void fireMessage(String message) {
		for(PipelineListener listener : listeners) {
			listener.message(message);
		}
	}
	
	
	public List<Operator> getOperatorList() {
		if (handler == null) {
			return new ArrayList<Operator>();
		}
		else {
			return handler.operatorList;
		}
	}
	
	
	
	public static void main(String[] args) {
		
		//args = new String[]{"/home/brendan/HHT3_downsample/globtest.xml"};
		
		//If no args, show the GUI window
		if (args.length == 0) {
			PipelineApp.showMainWindow();
			return;
		}
		
		ArgumentParser argParser = new ArgumentParser();
		argParser.parse(args);
		
		
		String check = argParser.getStringOp("check");
		boolean checkAndExit = false;
		if (check != null) {
			System.out.println("Checking input file only.");
			checkAndExit = true;
		}
		else {
			System.out.println("Executing file normally (check mode off)");
		}
		
		String projHome = argParser.getStringOp("home");
		if (projHome != null && (!projHome.endsWith("/"))) {
			projHome = projHome + "/";
		}

		//File to obtain properties from
		String propsPath = argParser.getStringOp("props");
		
		
		String threadCountStr = argParser.getStringOp("threads");
		int threads = -1; //Use value from properties file if possible
		if (threadCountStr != null) {
			threads = Integer.parseInt(threadCountStr);
		}
		
		//Assume all args that end in .xml are input files and execute them in order
		for(int i=0; i<args.length; i++) {
			if (args[i].endsWith(".xml")) {
				File input = new File(args[i]);
				Pipeline pipeline = new Pipeline(input, propsPath);
				
				//Set project home
				if (projHome != null && projHome.length()>0)
					pipeline.setProperty("home", projHome);
				
				//Set preferred thread count
				if (threads > -1) {
					pipeline.setProperty(PipelineXMLConstants.THREADS_ATTR, "" + threads);
				}
				
				try {
					pipeline.initializePipeline();
					
					if (checkAndExit) {
						System.out.println("SUCCESS: Pipeline initialized properly");
					}
					else {
						pipeline.execute();
					}
				} catch (PipelineDocException e) {
					e.printStackTrace();
					System.out.println("ERROR: Could not properly parse input document. \n" + e.getMessage());
					e.printStackTrace(System.out);
				} catch (ObjectCreationException e) {
					e.printStackTrace();
					System.out.println("ERROR: Could not create some objects \n" + e.getMessage());
					e.printStackTrace(System.out);
				} catch (OperationFailedException e) {
					System.out.println("ERROR: Operation " + e.getSourceOperator().getObjectLabel() + " failed. \n" + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
		}
	}
	
	
	
	
	private List<PipelineListener> listeners = new ArrayList<PipelineListener>();


}
