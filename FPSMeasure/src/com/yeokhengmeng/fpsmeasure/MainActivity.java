package com.yeokhengmeng.fpsmeasure;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";
	public static final String FPS_COMMAND = "dumpsys SurfaceFlinger --latency SurfaceView";
	public TextView outputView;

	public static final float TIME_INTERVAL_NANO_SECONDS = 1000000000;

	Process process;
	OutputStream stdin;
	InputStream stdout;
	BufferedReader reader;

	private ScheduledExecutorService scheduler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		outputView = (TextView) findViewById(R.id.output);
	}

	public void startButtonPress(View view){
		Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();

		
		scheduler = Executors.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate
		(new Runnable() {
			public void run() {
				int fps = getFPS(TIME_INTERVAL_NANO_SECONDS);
				//outputView.setText(Integer.toString(fps));
				Log.i(TAG, Integer.toString(fps));
			}
		}, 0, 1, TimeUnit.SECONDS);


	}

	public void stopButtonPress(View view){
		Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
		if(scheduler != null){
			scheduler.shutdownNow();
			scheduler = null;

		}
	}


	public void getRoot(View view){
		//		List<String> dummy = new ArrayList<String>();
		//		dummy.add("ls");
		//		runAsRoot(dummy);
		runStaticCommandAsRoot("ls");
	}
	
	
    /* 
     From: http://src.chromium.org/svn/branches/1312/src/build/android/pylib/surface_stats_collector.py 
     adb shell dumpsys SurfaceFlinger --latency <window name>
     prints some information about the last 128 frames displayed in
     that window.
     The data returned looks like this:
     16954612
     7657467895508   7657482691352   7657493499756
     7657484466553   7657499645964   7657511077881
     7657500793457   7657516600576   7657527404785
     (...)
    
     The first line is the refresh period (here 16.95 ms), it is followed
     by 128 lines w/ 3 timestamps in nanosecond each:
     A) when the app started to draw
     B) the vsync immediately preceding SF submitting the frame to the h/w
     C) timestamp immediately after SF submitted that frame to the h/w
    
	*/
	public int getFPS(double timeIntervalNanoSeconds){
		List<String> output = getFPSCommandOutput();
		
		if(output.size() == 0){
			return -1;
		}
		
		//First line is not used
		
		String lastLine = output.get(128);
		String[] split = splitLine(lastLine);
		String lastFrameFinishTimeStr = split[2];
		
		double lastFrameFinishTime = Double.parseDouble(lastFrameFinishTimeStr);
		int frameCount = 0;
		
		for(int i = 1; i <= 128 ; i++){
			String[] splitted = output.get(i).split("\t");
			String thisFrameFinishTimeStr = splitted[2];
			double thisFrameFirstTime = Double.parseDouble(thisFrameFinishTimeStr);
			if((lastFrameFinishTime - thisFrameFirstTime) <= timeIntervalNanoSeconds){
				frameCount++;
			}
			
		}
		return frameCount;
	}
	
	public String[] splitLine(String input){
		String[] result = input.split("\t");
		return result;
	}


	public List<String> getFPSCommandOutput(){
		try {
			process = Runtime.getRuntime().exec("su");
			stdin = process.getOutputStream();
			stdout = process.getInputStream();
			
			stdin.write((FPS_COMMAND + "\n").getBytes());
			stdin.write("exit\n".getBytes());
			stdin.flush();
			stdin.close();


			BufferedReader br =
					new BufferedReader(new InputStreamReader(stdout));

			
			ArrayList<String> output = new ArrayList<String>();
			String line;
			
			while ((line = br.readLine()) != null) {
				output.add(line);
			}
			
			
			
			process.waitFor();
			process.destroy();
			
			
		
			return output;

		} catch (Exception ex) {
			return new ArrayList<String>();
		}
	}


	public String runStaticCommandAsRoot(String command){


		try {
			String line;
			StringBuilder log=new StringBuilder();
			Process process = Runtime.getRuntime().exec("su");
			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();

			stdin.write((command + "\n").getBytes());
			stdin.write("exit\n".getBytes());
			stdin.flush();

			stdin.close();
			BufferedReader br =
					new BufferedReader(new InputStreamReader(stdout));
			while ((line = br.readLine()) != null) {
				Log.d("[Output]", line);
				log.append(line);
				log.append("\n");
			}
			br.close();
			process.waitFor();
			process.destroy();


			return log.toString();
		} catch (Exception ex) {
			return "";
		}

	}

}
