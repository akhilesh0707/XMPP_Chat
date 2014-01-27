package com.example.samsungxmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ListView;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;


public class XMPPChatDemoActivity extends Activity {

	public static final String HOST = "talk.google.com";
	public static final int PORT = 5222;
	public static final String SERVICE = "gmail.com";
	public String USERNAME;
	public String PASSWORD;
	
	private XMPPConnection connection;
	private ArrayList<String> messages = new ArrayList<String>();
	private ArrayList<String> messages_filtered = new ArrayList<String>();
	private Handler mHandler = new Handler();

	private EditText recipient;
	private EditText textMessage;
	private ListView listview;
	private ArrayList<String> emails = new ArrayList<String>();
	
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mTagTitles;
    
    private int side;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		USERNAME = getIntent().getStringExtra("user");
		PASSWORD = getIntent().getStringExtra("pass");
		
		recipient = (EditText) this.findViewById(R.id.toET);
		textMessage = (EditText) this.findViewById(R.id.chatET);
		listview = (ListView) this.findViewById(R.id.listMessages);
		setListAdapter();
		
		// Get a reference to the AutoCompleteTextView in the layout
		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.toET);
		// Create the adapter and set it to the AutoCompleteTextView 
		ArrayAdapter<String> adapter = 
		        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emails);
		textView.setAdapter(adapter);

		// Set a listener to send a chat text message
		Button send = (Button) this.findViewById(R.id.sendBtn);
		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String to = recipient.getText().toString();
				String text = textMessage.getText().toString();

				Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);				
				if (connection != null) {
					connection.sendPacket(msg);
					messages.add(connection.getUser() + ":");
					messages.add(text);
					selectItem(side);
				}
				EditText chatET=(EditText) findViewById(R.id.chatET);
				chatET.setText("");
				
			}
		});

		connect();
		
		// Navigation drawer start----------------------------
    	mTitle = mDrawerTitle = getTitle();
    	mTagTitles = getResources().getStringArray(R.array.tag_array);
    	mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    	mDrawerList = (ListView) findViewById(R.id.left_drawer);

    	// set a custom shadow that overlays the main content when the drawer opens
    	mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    	// set up the drawer's list view with items and click listener
    	mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mTagTitles));
    	mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

    	// enable ActionBar app icon to behave as action to toggle nav drawer
    	getActionBar().setDisplayHomeAsUpEnabled(true);
    	getActionBar().setHomeButtonEnabled(true);

    	// ActionBarDrawerToggle ties together the the proper interactions
    	// between the sliding drawer and the action bar app icon
    	mDrawerToggle = new ActionBarDrawerToggle(
    			this,                  /* host Activity */
    			mDrawerLayout,         /* DrawerLayout object */
    			R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
    			R.string.drawer_open,  /* "open drawer" description for accessibility */
    			R.string.drawer_close  /* "close drawer" description for accessibility */
    			) {
    		public void onDrawerClosed(View view) {
    			getActionBar().setTitle(mTitle);
    			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    		}

    		public void onDrawerOpened(View drawerView) {
    			getActionBar().setTitle(mDrawerTitle);
    			invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    		}
    	};
    	mDrawerLayout.setDrawerListener(mDrawerToggle);
    	
    	if (savedInstanceState == null) {
    		selectItem(0);
    	}

	}
    	
        /* The click listener for ListView in the navigation drawer */
        private class DrawerItemClickListener implements ListView.OnItemClickListener {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        }
        
    	private void selectItem(int position) {
    		// update the main content by filtering through messages
    		int i = 0;
    		if (position == 0) {
    		    setListAdapter();
    		}
    		else {
    			i = 0;
    			messages_filtered.clear();
    			for(String line : messages){
    				if (line.toLowerCase().contains(mTagTitles[position])){
    					
    					//sender
    					messages_filtered.add(messages.get(i-1));
    					
    					//message
    					messages_filtered.add(messages.get(i));
        			}
    				i++;
    			}
    			setFilteredListAdapter();
    			
    		}

            // update selected item and title, then close the drawer
    		side = position;
            mDrawerList.setItemChecked(position, true);
            setTitle(mTagTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);
    	}
        
    	// Navigation drawer end----------------------------------
    	
    	
		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				
			case R.id.action_settings:
				setConnection(null);
				//connection.disconnect();
				Intent intent = new Intent(this, LoginActivity.class);
				startActivity(intent);
				return true;
				
			}
			return super.onOptionsItemSelected(item);
	}

	/**
	 * Called by Settings dialog when a connection is established with the XMPP
	 * server
	 * 
	 * @param connection
	 */
	public void setConnection(XMPPConnection connection) {
		this.connection = connection;
		if (connection != null) {
			// Add a packet listener to get messages sent to us
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			connection.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						String fromName = StringUtils.parseBareAddress(message
								.getFrom());
						Log.i("XMPPChatDemoActivity", "Text Recieved " + message.getBody()
								+ " from " + fromName );
						messages.add(fromName + ":");
						messages.add(message.getBody());
						// Add the incoming message to the list view
						mHandler.post(new Runnable() {
							public void run() {
								selectItem(side);
							}
						});
					}
				}
			}, filter);
		}
	}

	private void setListAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem, messages);
		listview.setAdapter(adapter);
	}
	
	private void setFilteredListAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem, messages_filtered);
		listview.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (connection != null)
				connection.disconnect();
		} catch (Exception e) {

		}
	}

	public void connect() {

		final ProgressDialog dialog = ProgressDialog.show(this,
				"Connecting...", "Please wait...", false);

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// Create a connection
				ConnectionConfiguration connConfig = new ConnectionConfiguration(
						HOST, PORT, SERVICE);
				XMPPConnection connection = new XMPPConnection(connConfig);

				try {
					connection.connect();
					Log.i("XMPPChatDemoActivity",
							"Connected to " + connection.getHost());
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to connect to "
							+ connection.getHost());
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
					runOnUiThread(new Runnable() 
					{
					   public void run() 
					   {
						   Toast.makeText(getApplicationContext(), "Failed to connect. Please try again.", Toast.LENGTH_SHORT).show();
						   onBackPressed();    
					   }
					}); 
				}
				try {
					// SASLAuthentication.supportSASLMechanism("PLAIN", 0);
					connection.login(USERNAME, PASSWORD);
					Log.i("XMPPChatDemoActivity",
							"Logged in as " + connection.getUser());

					// Set the status to available
					Presence presence = new Presence(Presence.Type.available);
					connection.sendPacket(presence);
					setConnection(connection);

					Roster roster = connection.getRoster();
					Collection<RosterEntry> entries = roster.getEntries();
					for (RosterEntry entry : entries) {
						Log.d("XMPPChatDemoActivity",
								"--------------------------------------");
						Log.d("XMPPChatDemoActivity", "RosterEntry " + entry);
						Log.d("XMPPChatDemoActivity",
								"User: " + entry.getUser());
						// String array for AutoCompleteTextView
						emails.add(entry.getUser());
						Log.d("XMPPChatDemoActivity",
								"Name: " + entry.getName());
						Log.d("XMPPChatDemoActivity",
								"Status: " + entry.getStatus());
						Log.d("XMPPChatDemoActivity",
								"Type: " + entry.getType());
						Presence entryPresence = roster.getPresence(entry
								.getUser());

						Log.d("XMPPChatDemoActivity", "Presence Status: "
								+ entryPresence.getStatus());
						Log.d("XMPPChatDemoActivity", "Presence Type: "
								+ entryPresence.getType());
						Presence.Type type = entryPresence.getType();
						if (type == Presence.Type.available)
							Log.d("XMPPChatDemoActivity", "Presence AVIALABLE");
						Log.d("XMPPChatDemoActivity", "Presence : "
								+ entryPresence);

					}
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to log in as "
							+ USERNAME);
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
					runOnUiThread(new Runnable() 
					{
					   public void run() 
					   {
						   Toast.makeText(getApplicationContext(), "Failed to log in as "
									+ USERNAME + ". Please reenter information and try again.", Toast.LENGTH_SHORT).show();
						   onBackPressed();    
					   }
					}); 
				}

				dialog.dismiss();
			}
		});
		t.start();
		dialog.show();
				
	}
}