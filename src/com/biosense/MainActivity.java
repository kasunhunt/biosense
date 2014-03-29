package com.biosense;

import com.biosense.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements OnItemClickListener {
	//charting
	private GraphicalView mChart;
	private XYSeries visitsSeries ;
	private XYMultipleSeriesDataset dataset;
	private XYSeriesRenderer visitsRenderer;
	private XYMultipleSeriesRenderer multiRenderer;
	int i =1;
	int max = 0, min =-99;
	
	
	//edit
    TextView timeView;
    TextView temperatureView;
    String mainText;
    String prevText = "0000000000";
    BluetoothSocket mmSocket2;
    Button buttonSend;
    Button buttonBt;

    
    ArrayAdapter<String> listAdapter;
    ListView listView;
	BluetoothAdapter btAdapter;
	Set<BluetoothDevice> devicesArray;
	ArrayList<String> pairedDevices;
	ArrayList<BluetoothDevice> devices;
	IntentFilter filter;
	BroadcastReceiver receiver;
	private ConnectedThread mConnectedThread;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	protected static final int SUCCESS_CONNECT = 0;
	protected static final int MESSAGE_READ  = 1;
    private final static int REQUEST_ENABLE_BT = 1;
	String tag = "DEBUG";
	Handler mHandler = new Handler(){
		
    	@Override
    	public void handleMessage(Message msg) {

    		super.handleMessage(msg);
    		switch(msg.what){
    		case SUCCESS_CONNECT:
    			//Do something
    			ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
    			Toast.makeText(getApplicationContext(), "CONNECTED", 0).show();
    			String s = "SND"; //request monitoring
    			connectedThread.write(s.getBytes());
    			Log.i(tag, "connected"); 
    			break;
    		case MESSAGE_READ:
    			//byte[] readBuf = (byte[])msg.obj;
    			//String string = new String (readBuf);
    	        String readMessage = (String) msg.obj;
    	        
    			mainText = readMessage.substring(0,1);
    			Log.i(tag, readMessage);
    			
    			
    			String[] sample = {"0","1","2","3","4","5","6","7","8","9"};
    			for (int xxx = 0; xxx<sample.length; xxx++){
    				//Log.i(tag, "true2");
    				if (sample[xxx].equals(mainText)){
    					//Log.i(tag, "true3");
    					onProgressUpdate(mainText);	
    					
    				}
    			}
    								
    			break;
    		}
    	}


    };
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        
        // Setting up chart
        setupChart();
        
        // Start plotting chart
        new ChartTask().execute();
        
        init();
        bluetoothInit(); 
    }
    
    
    private void bluetoothInit() {
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
    	
    	if (btAdapter==null){
        	Toast.makeText(getApplicationContext(), "No bluetooth adapter detected", 0).show();
        	finish();
        }
        else{
        	 if (!btAdapter.isEnabled()){
        		 turnOnBT();	  
        	 }
        	 getPairedDevices();
             startDiscovery();
        }
	}
    
	public void onProgressUpdate(String values) {
    	//Chart a integer value
    	
    	multiRenderer.setXAxisMax(max+i);
    	multiRenderer.setXAxisMin(min+i);
    	Log.i(tag, values+"b");
		visitsSeries.add(i, Integer.parseInt(values));
		mChart.repaint();
		if (i == -1 ){
			i =0;
		}
		i++;
		
		Log.i(tag, "mchart repaing");
	}
    
    private void setupChart(){
    	
    	// Creating an  XYSeries for Visits
    	visitsSeries = new XYSeries("Biodata");
    	
    	
    	// Creating a dataset to hold each series
    	dataset = new XYMultipleSeriesDataset();
    	// Adding Visits Series to the dataset
    	dataset.addSeries(visitsSeries);    	
    	
    	
    	// Creating XYSeriesRenderer to customize visitsSeries
    	visitsRenderer = new XYSeriesRenderer();
    	visitsRenderer.setColor(Color.BLACK);
    	visitsRenderer.setPointStyle(PointStyle.POINT);
    	visitsRenderer.setFillPoints(true);
    	visitsRenderer.setLineWidth(2);
    	visitsRenderer.setDisplayChartValues(false);
    	
    	
    	// Creating a XYMultipleSeriesRenderer to customize the whole chart
    	multiRenderer = new XYMultipleSeriesRenderer();
    	
    	multiRenderer.setChartTitle("");
    	multiRenderer.setXTitle("Time");
    	multiRenderer.setYTitle("Count");
    	multiRenderer.setZoomButtonsVisible(true);
    	multiRenderer.setInScroll(true);

    	
    	multiRenderer.setYAxisMin(0);
    	multiRenderer.setYAxisMax(10);
    	
    	multiRenderer.setBarSpacing(0);
    	
    	// Adding visitsRenderer to multipleRenderer
    	// Note: The order of adding dataseries to dataset and renderers to multipleRenderer
    	// should be same
    	multiRenderer.addSeriesRenderer(visitsRenderer);
    	
    	// Getting a reference to LinearLayout of the MainActivity Layout
    	LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart_container);
    	
   		
    	//mChart = (GraphicalView) ChartFactory.getBarChartView(getBaseContext(), dataset, multiRenderer, Type.DEFAULT);
    	mChart = (GraphicalView) ChartFactory.getLineChartView(getBaseContext(), dataset, multiRenderer);
    	
   		// Adding the Line Chart to the LinearLayout
    	chartContainer.addView(mChart);
    	
    }
    
    
    public class ChartTask extends AsyncTask<Void, String, Void>{

    	// Generates dummy data in a non-ui thread
		@Override
		protected Void doInBackground(Void... params) {
			
			return null;
		}
		
		// Plotting generated data in the graph
		
    	
    }    

    private void startDiscovery() {
		// TODO Auto-generated method stub
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
	}


	private void turnOnBT() {
		// TODO Auto-generated method stub
    	Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(intent,REQUEST_ENABLE_BT);
	}


	private void getPairedDevices() {
		// TODO Auto-generated method stub
		devicesArray = btAdapter.getBondedDevices();
		if (devicesArray.size()>0){
			for(BluetoothDevice device:devicesArray){
				pairedDevices.add(device.getName());
			}	
		}
	}


	private void init() {
		// TODO Auto-generated method stub
		//connectNew = (Button)findViewById(R.id.bConnectNew);
		
		listView = (ListView)findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);
		listView.setAdapter(listAdapter);
		
		pairedDevices = new ArrayList<String>();
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		devices = new ArrayList<BluetoothDevice>();
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				if(BluetoothDevice.ACTION_FOUND.equals(action)){
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					devices.add(device);
					String s = "";
						//for(int i = 0;i<listAdapter.getCount();i++){
							for (int a = 0;a<pairedDevices.size();a++){
								if(device.getName().equals(pairedDevices.get(a))){
									//append
									s = "(Paired)";
									break;
								}
							}
						//}
						//ex:matt_hp (paired)
					listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());

					
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
					//run some code
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

				}
				else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
					if (btAdapter.getState() == btAdapter.STATE_OFF){
						turnOnBT();
					}
				}
			}
		};
		
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(receiver, filter);


	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unregisterReceiver(receiver);
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==RESULT_CANCELED){
			Toast.makeText(getApplicationContext(),"Bluetooth must be enabled to continue",Toast.LENGTH_SHORT).show();
			finish();
		}
	}


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		if(btAdapter.isDiscovering()){
			btAdapter.cancelDiscovery();
		}
		Toast.makeText(getApplicationContext(), listAdapter.getItem(arg2),0).show();

		if(listAdapter.getItem(arg2).contains("Paired")){
			BluetoothDevice selectedDevice = devices.get(arg2);
			//Toast.makeText(getApplicationContext(), "device is paired",0).show();	
			ConnectThread connect = new ConnectThread(selectedDevice);
			connect.start();
			//Log.i(tag,"in click listner");
			
			//edit
			//ConnectedThread connected = new ConnectedThread(selectedDevice);
			
		}
		else {
			Toast.makeText(getApplicationContext(), "Device Is Not Paired",0).show();
		}
		
	}
	
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	        mmSocket2 = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery beca use it will slow down the connection
	        btAdapter.cancelDiscovery();
	        Log.i(tag,"connect-run");
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            Log.i(tag, "connect - succeeded");
	        } catch (IOException connectException) { Log.i(tag, "connect - failed");
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
	        connected(mmSocket);//edit
	    }
	 


		/** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer;  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	            	buffer  = new byte[1024];
	                bytes = mmInStream.read(buffer);
	                String readMessage = new String(buffer, 0, bytes);

	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, readMessage) //readMessage replaces buffer
	                        .sendToTarget();
	                
	            } catch (IOException e) {
	                break;
	            }
	           //SystemClock.sleep(300);

	        }

	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	} 
	
	/**  */
	public void sendMessage(View view) {
	    // Do something in response to button
		Date d = new Date();
		//CharSequence s  = DateFormat.format("hhmmssMddyy", d.getTime());
		CharSequence s  = DateFormat.format("yyyyMddhhmmss", d.getTime());
	    			ConnectedThread connectedThread2 = new ConnectedThread(mmSocket2);
	    			//Toast.makeText(getApplicationContext(), "CONNECTED", 0).show();

	    			connectedThread2.write(((String) s).getBytes());
	    			Log.i(tag, "DATE Time"); 
	    			
	}
	
	/**  */
	public void sendGet(View view) {
	    // Do something in response to button
		
		Handler mHandler = new Handler(){
			
	    	@Override
	    	public void handleMessage(Message msg) {

	    		super.handleMessage(msg);
	    		switch(msg.what){
	    		case SUCCESS_CONNECT:
	    			//Do something
	    			ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
	    			Toast.makeText(getApplicationContext(), "CONNECTED", 0).show();
	    			String s = "GET"; //request monitoring
	    			connectedThread.write(s.getBytes());
	    			Log.i(tag, "connected"); 
	    			break;
	    		case MESSAGE_READ:
	    			//byte[] readBuf = (byte[])msg.obj;
	    			//String string = new String (readBuf);
	    	        String readMessage = (String) msg.obj;

	    	        
	    		   

	    		}
	    	}};}
	
	public void startBtDiscovery(View view) {
	    // Do something in response to button
		listAdapter.clear();
		startDiscovery();
	}
	

	public synchronized void connected(BluetoothSocket socket) {

	    mConnectedThread = new ConnectedThread(socket);
	    mConnectedThread.start();
	}

	
    
    
}