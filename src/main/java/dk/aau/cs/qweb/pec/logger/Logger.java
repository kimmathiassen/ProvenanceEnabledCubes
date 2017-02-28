package dk.aau.cs.qweb.pec.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Logger {
	private File file = new File("offline.log");
	private Map<String, Long> timers;
	StringBuilder sb;
	private boolean isDisabled = false;
	
	public Logger() {
		timers = new HashMap<String,Long>();
		sb = new StringBuilder();;
	}
	
	public Logger (File file) {
		this();
		this.file = file;
	}
	
	public Logger (String file) {
		this();
		this.file = new File(file);
	}

	
	public void log(String string) {
		sb.append(string);
		sb.append(System.getProperty("line.separator"));
	}
	
//	public void log(String string,Object value) {
//		sb.append(string+": "+value);
//		sb.append(System.getProperty("line.separator"));
//	}
	
	public void log(String string,long value) {
		sb.append(string+": "+String.valueOf(value));
		sb.append(System.getProperty("line.separator"));
	}

	public void startTimer(String string) {
		timers.put(string, System.currentTimeMillis());
	}

	public void endTimer(String string) {
		long end = System.currentTimeMillis();
		if (timers.containsKey(string)) {
			Long start = timers.get(string);
			long time = end-start;
			sb.append(string+": "+time+" ms");
			sb.append(System.getProperty("line.separator"));
		} 
	}

	public void write() {
		if (isDisabled) {
			return;
		}
		
		this.clear();
		BufferedWriter bw = null;
		FileWriter fw = null;
		sb.append(System.getProperty("line.separator"));
		
		try {

			if (!file.exists()) {
				file.createNewFile();
			}

			fw = new FileWriter(file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			bw.write(sb.toString());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	private void clear() {
		// TODO Auto-generated method stub
		
	}

	public void disable() {
		isDisabled  = true;
	}

}
