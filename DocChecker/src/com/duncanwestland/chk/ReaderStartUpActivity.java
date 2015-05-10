package com.duncanwestland.chk;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.Cipher;
import com.duncanwestland.lsr2.APDU;
import com.duncanwestland.lsr2.DG1;
import com.duncanwestland.lsr2.DG2;
import com.duncanwestland.lsr2.EFSOD;
import com.duncanwestland.chk.R;
import com.duncanwestland.lsr2.SecureMessageAPDU;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
//import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ReaderStartUpActivity extends Activity {
	/** Called when the activity is first created. */
	static final int DOB_DIALOG_ID = 0;
	static final int DOE_DIALOG_ID = 1;
	static final int WARNING_DIALOG_ID = 2;
	static final int HELP_DIALOG_ID = 3;
	static final long WAIT_TO_READ = 2000;
	static final byte[] KEY_TYPE_ENC = { 0x00, 0x00, 0x00, 0x01 };
	static final byte[] KEY_TYPE_MAC = { 0x00, 0x00, 0x00, 0x02 };
	public static final String DEBUG = "crypto";
	public static final String PREFERENCES = "preferences";
	public static final String PREFERENCES_NO_SHOW = "noShow";
	TextView textViewDobSet;
	TextView textViewDoeSet;
	EditText editTextDoc;
	SharedPreferences preferences;
	NfcAdapter nfcAdapter;
	PendingIntent pendingIntent;
	IsoDep tcvr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.startup);
		ReaderStartUpActivity.this.setProgressBarIndeterminateVisibility(false);

		// set up shared preferences
		preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

		// create GUI objects#
		Button buttonDob = (Button) findViewById(R.id.buttonDob);
		Button buttonDoe = (Button) findViewById(R.id.buttonDoe);
		textViewDobSet = (TextView) findViewById(R.id.textViewDobSet);
		textViewDoeSet = (TextView) findViewById(R.id.textViewDoeSet);
		editTextDoc = (EditText) findViewById(R.id.editTextDoc);

		// create an NFC adaptor and start listening for tags
		NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
		nfcAdapter = nfcManager.getDefaultAdapter();
		pendingIntent = PendingIntent.getActivity(ReaderStartUpActivity.this,
				0, new Intent(ReaderStartUpActivity.this, getClass())
						.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// display warning screen unless you've been told not to
		// Log.i(DEBUG,"doing show dialog test"+
		// preferences.getBoolean(PREFERENCES_NO_SHOW, false));
		if (!preferences.getBoolean(PREFERENCES_NO_SHOW, false))
			showDialog(WARNING_DIALOG_ID);

		// DoB entry button pressed
		buttonDob.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DOB_DIALOG_ID);
			}
		});
		// DoE entry button pressed
		buttonDoe.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DOE_DIALOG_ID);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		// menu.findItem(R.id.item_help).setIntent(new
		// Intent(this,HelpActivity.class));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		// there is only one menu item so we must have selected it
		// if more items are added then a test needs to be placed here
		showDialog(HELP_DIALOG_ID); // temp - just to make it do something
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		Calendar cal = Calendar.getInstance();
		int iDay = cal.get(Calendar.DAY_OF_MONTH);
		int iMonth = cal.get(Calendar.MONTH);
		int iYear = cal.get(Calendar.YEAR);
		switch (id) {
		case DOB_DIALOG_ID:
			DatePickerDialog dobDialog = new DatePickerDialog(this,
					new DatePickerDialog.OnDateSetListener() {
						public void onDateSet(DatePicker view, int year,
								int monthOfYear, int dayOfMonth) {
							Time dob = new Time();
							// fix to give my dob
							// dayOfMonth = 5;monthOfYear=0;year=1980;
							dob.set(dayOfMonth, monthOfYear, year);
							BioData.dateOfBirth = dob.toMillis(true);
							textViewDobSet.setText((String) DateFormat.format(
									"MMMM dd, yyyy", BioData.dateOfBirth));
						}
					}, iYear, iMonth, iDay);
			return dobDialog;
		case DOE_DIALOG_ID:
			DatePickerDialog doeDialog = new DatePickerDialog(this,
					new DatePickerDialog.OnDateSetListener() {
						public void onDateSet(DatePicker view, int year,
								int monthOfYear, int dayOfMonth) {
							Time dob = new Time();
							// fix to give kirit doe
							// dayOfMonth = 1;monthOfYear=0;year=2014;
							dob.set(dayOfMonth, monthOfYear, year);
							BioData.dateOfExpiry = dob.toMillis(true);
							textViewDoeSet.setText((String) DateFormat.format(
									"MMMM dd, yyyy", BioData.dateOfExpiry));
						}
					}, iYear, iMonth, iDay);
			return doeDialog;

		case WARNING_DIALOG_ID:
			// create the warning dialog view
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.warning,
					(ViewGroup) findViewById(R.id.root));
			// set an event listener on the check box
			CheckBox checkBoxNoShow = (CheckBox) layout
					.findViewById(R.id.checkBoxNoShow);
			checkBoxNoShow
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							// store the fact that the checkbox has been ticked
							// Log.i(DEBUG,"setting noShow True");
							Editor editor = preferences.edit();
							editor.putBoolean(PREFERENCES_NO_SHOW, true);
							editor.commit();
							// if (preferences.getBoolean(PREFERENCES_NO_SHOW,
							// false)) Log.d(DEBUG,"no show IS true");
						}
					});
			// add it to an alert dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			builder.setTitle(R.string.app_name);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							ReaderStartUpActivity.this
									.removeDialog(WARNING_DIALOG_ID);
						}
					});
			AlertDialog warningDialog = builder.create();
			return warningDialog;
		case HELP_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name);
			String help = getResources().getString(R.string.help);
			String version = getResources().getString(R.string.version);
			String message = help + "\n\n" + version;
			builder.setMessage(message);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							ReaderStartUpActivity.this
									.removeDialog(HELP_DIALOG_ID);
						}
					});
			AlertDialog helpDialog = builder.create();
			return helpDialog;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		int iDay, iMonth, iYear;
		switch (id) {
		case DOB_DIALOG_ID:
			// handle any DatePickerDialog intitialisation here
			DatePickerDialog dobDialog = (DatePickerDialog) dialog;
			// check for existing Date of Birth
			/**
			 * if (BioData.dateOfBirth!=0) { Time dob = new Time();
			 * dob.set(BioData.dateOfBirth); iDay = dob.monthDay; iMonth =
			 * dob.month; iYear = dob.year; } else {
			 **/
			Calendar cal = Calendar.getInstance();
			iDay = cal.get(Calendar.DAY_OF_MONTH);
			iMonth = cal.get(Calendar.MONTH);
			iYear = cal.get(Calendar.YEAR);
			// }
			dobDialog.updateDate(iYear, iMonth, iDay);
			return;
		case DOE_DIALOG_ID:
			// handle any DatePickerDialog intitialisation here
			DatePickerDialog doeDialog = (DatePickerDialog) dialog;
			// check for existing Date of Expiry
			/**
			 * if (BioData.dateOfExpiry!=0) { Time dob = new Time();
			 * dob.set(BioData.dateOfExpiry); iDay = dob.monthDay; iMonth =
			 * dob.month; iYear = dob.year; } else {
			 **/
			// final TextView textViewDoeSet = (TextView)
			// findViewById(R.id.textViewDoeSet);
			// textViewDoeSet.setText(getResources().getString(R.string.noDate));
			Calendar cal1 = Calendar.getInstance();
			iDay = cal1.get(Calendar.DAY_OF_MONTH);
			iMonth = cal1.get(Calendar.MONTH);
			iYear = cal1.get(Calendar.YEAR);
			// }
			doeDialog.updateDate(iYear, iMonth, iDay);
			return;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		nfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	public void onStop() {
		super.onStop();
		textViewDobSet.setText(getResources().getString(R.string.noDate));
		textViewDoeSet.setText(getResources().getString(R.string.noDate));
		editTextDoc.setText(null);
		//BioData.clear();
	}
	@Override
	public void onNewIntent(Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		// when a tag is found, create a transceiver and a passport reader to
		// read it
		tcvr = IsoDep.get(tag);
		// read data from the GUI and check MRZ data is good to go
		// BRP - mod made to add uppercase conversion
		BioData.passportNumber = editTextDoc.getText().toString().toUpperCase();
		if (!BioData.checkMRZ()) {
			Toast.makeText(ReaderStartUpActivity.this, "Error in MRZ data",
					Toast.LENGTH_LONG).show();
			return;
		}
		// instantiate a reader class and (asynchronously) read the passport
		// Log.d(DEBUG,"tag found - launching reader");
		PassportReader passportReader = new PassportReader();
		passportReader.execute();
	}

	/**
	 * Class that represents a passport reader
	 * 
	 * @author duncan
	 * 
	 */

	private class PassportReader extends AsyncTask<Object, String, Boolean> {
		// class to run passport reading asynchronously
		/**
		 * helper class to process a response byte - not needed in Python
		 * 
		 * @author duncan
		 * 
		 */
		private class Response {
			public byte[] data;
			public byte sw1;
			public byte sw2;

			private void setResponse(byte[] resp) {
				Crypto.logger("Response was ", resp);
				data = Arrays.copyOfRange(resp, 0, resp.length - 2);
				sw2 = resp[resp.length - 1];
				sw1 = resp[resp.length - 2];
				if (sw1 != -0x70 | sw2 != 0x00) {
					// Log.e(DEBUG,"response bytes weren't 0x90 0x00: "
					// + Crypto.hex(new byte[]{sw1})+ Crypto.hex(new
					// byte[]{sw2}));

				}
			}
		}

		@Override
		protected void onPreExecute() {
			ReaderStartUpActivity.this
					.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// the asynchronous reading has finished, so the results need to be
			// displayed
			ReaderStartUpActivity.this
					.setProgressBarIndeterminateVisibility(false);
			if (result) { // there has been some error reading the passport
				Toast.makeText(
						ReaderStartUpActivity.this,
						"Error reading passport. Please check typing and adjust the passport's position. ",
						Toast.LENGTH_LONG).show();
				// BioData.clear();
				try {
					tcvr.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			} else {
				startActivity(new Intent(getApplicationContext(),
						PassportSummaryActivity.class));
			}
		}

		@Override
		protected Boolean doInBackground(Object... params) {
			// Read the passport's chip and update the BioData class's static
			// attributes
			try { // this is a general catch for any reader errors. It will
				Response resp = new Response();
				// compute the hash from the MRZ information
				byte[] mrzInfoBytes = BioData.getMrzInfo();
				Crypto.logger("mrzInfoBytes", mrzInfoBytes);
				byte[] mrzHash;
				try {
					mrzHash = Crypto.hash(mrzInfoBytes);
				} catch (NoSuchAlgorithmException e2) {
					return true;
				}
				// compute the key seed then Kenc and Kmac
				byte[] kSeed = Arrays.copyOfRange(mrzHash, 0, 16); // 0,16
				byte[] Kenc;
				try {
					Kenc = Crypto.makeKeys(kSeed, KEY_TYPE_ENC);
				} catch (NoSuchAlgorithmException e2) {
					return true;
				}
				byte[] Kmac;
				try {
					Kmac = Crypto.makeKeys(kSeed, KEY_TYPE_MAC);
				} catch (NoSuchAlgorithmException e2) {
					return true;
				}

				// connect to the ppt
				// if (tcvr.isConnected()) Log.d(DEBUG,"already connected");
				tcvr.setTimeout(10000);
				// try to connect 5 times
				int connectTrys = 1;
				final int CONNECT_MAX = 6;
				while (connectTrys < CONNECT_MAX) {
					try {
						// Log.d(DEBUG,"connecting....");
						tcvr.connect();
						break;
						// Log.i(DEBUG,"Tranceiver connected");
						// logger("Historical bytes",tcvr.getHistoricalBytes());
					} catch (IOException e1) {
						// Log.e(DEBUG, e1.getMessage());
						connectTrys++;
					}
				}
				if (connectTrys == CONNECT_MAX)
					return true; // failed to connect
				// request RNDicc number from passport
				byte[] RNDicc;
				try {
					// Log.d(DEBUG,"sending selectIssuerApplication");
					tcvr.transceive(APDU.selectIssuerApplication());
					// Log.d(DEBUG,"requesting reqRNDICC");
					resp.setResponse(tcvr.transceive(APDU.reqRNDICC()));
				} catch (IOException e) {
					// Log.e(DEBUG,e.getMessage());
				}
				RNDicc = resp.data;

				// generate random numbers
				SecureRandom random = new SecureRandom();
				byte[] RNDifd = new byte[8];
				byte[] Kifd = new byte[16];
				random.nextBytes(RNDifd);
				random.nextBytes(Kifd);

				// Concatenate RND.IFD, RND.ICC and KIFD
				byte[] S = Crypto.concat(RNDifd, RNDicc);
				S = Crypto.concat(S, Kifd);

				// DES3 encryption of S
				byte[] Eifd;
				try {
					Eifd = Crypto.DES3(S, Cipher.ENCRYPT_MODE, Kenc);
				} catch (GeneralSecurityException e1) {
					return true;
				}

				// Retail MAC of EIFD
				byte[] Mifd;
				try {
					Mifd = Crypto.MAC(Kmac, Eifd);
				} catch (GeneralSecurityException e1) {
					return true;
				}

				// Construct command for MUTUAL AUTHENTICATE and send
				byte[] cmdData = Crypto.concat(Eifd, Mifd);
				try {
					// Log.d(DEBUG,"sending mutualAuthenticate command");
					resp.setResponse(tcvr.transceive(APDU
							.mutualAuthenticate(cmdData)));
				} catch (IOException e) {
					// Log.e(DEBUG,e.getMessage());
					return true;
				}
				// decrypt and verify the response
				int split = resp.data.length - 8;
				byte[] Micc;
				byte[] Eicc;
				try { // after a crash force a check of
						// ArrayIndexOutOfBoundsException
					Micc = Arrays.copyOfRange(resp.data, split,
							resp.data.length);
					Eicc = Arrays.copyOfRange(resp.data, 0, split);
				} catch (ArrayIndexOutOfBoundsException e) {
					// not sure why this error happened but reset the reader if
					// it
					// does
					return true;
				}
				byte[] MiccCheck;
				try {
					MiccCheck = Crypto.MAC(Kmac, Eicc);
				} catch (GeneralSecurityException e1) {
					return true;
				}
				if (!Arrays.equals(MiccCheck, Micc)) {
					// Log.e(DEBUG, "Micc check error");
					Crypto.logger("Micc ", Micc);
					Crypto.logger("MiccCheck ", MiccCheck);
					return true;
				}
				byte[] R;
				try {
					R = Crypto.DES3(Eicc, Cipher.DECRYPT_MODE, Kenc);
				} catch (GeneralSecurityException e1) {
					return true;
				}
				byte[] RNDifdCheck = Arrays.copyOfRange(R, 8, 16);
				if (!Arrays.equals(RNDifdCheck, RNDifd)) {
					// Log.e(DEBUG, "RNDifd check error");
					return true;
				}

				// calculate session keys
				byte[] Kicc = Arrays.copyOfRange(R, 16, R.length);
				byte[] Kseed = new byte[Kicc.length];
				for (int i = 0; i < 16; i++)
					Kseed[i] = (byte) (Kifd[i] ^ Kicc[i]);
				byte[] KSenc;
				byte[] KSmac;
				try {
					KSenc = Crypto.makeKeys(Kseed, KEY_TYPE_ENC);
					KSmac = Crypto.makeKeys(Kseed, KEY_TYPE_MAC);
				} catch (GeneralSecurityException e1) {
					return true;
				}

				// calculate send sequence counter
				byte[] SSC = Crypto.concat(
						Arrays.copyOfRange(RNDicc, 4, RNDicc.length),
						Arrays.copyOfRange(RNDifd, 4, RNDifd.length));

				// instantiate a secure messaging APDU object
				// Log.d(DEBUG,"Instantiating secure messaging object");
				SecureMessageAPDU SMapdu = new SecureMessageAPDU();
				SMapdu.setKeys(KSenc, KSmac);
				SMapdu.startSSC(SSC);

				// at this point, secure messaging is established

				// create DG1
				// Log.d(DEBUG,"creating DG1");
				DG1 dg1;
				try {
					dg1 = new DG1(tcvr);
				} catch (GeneralSecurityException e1) {
					return true;
				} catch (IOException e1) {
					return true;
				}
				dg1.parseBioData(); // load relevant data into the BioData class

				// create DG2
				// Log.d(DEBUG,"creating DG2");
				DG2 dg2;
				try {
					dg2 = new DG2(tcvr);
				} catch (GeneralSecurityException e1) {
					return true;
				} catch (IOException e1) {
					return true;
				}
				dg2.parseBioData();

				// create EF.SOD
				// Log.d(DEBUG,"creating EF.SOD");
				EFSOD efSOD;
				try {
					efSOD = new EFSOD(tcvr);
				} catch (GeneralSecurityException e1) {
					return true;
				} catch (IOException e1) {
					return true;
				}

				// close the nfc transceiver
				try {
					tcvr.close();
				} catch (IOException e) {
					// Log.e(DEBUG,"IOException on closing tcvr");
					// e.printStackTrace();
				}
				// load EF data into the BioData class; this lets it be picked
				// up
				// by PassportSummaryActivity
				BioData.efSOD = efSOD.getEncoded();
				try {
					BioData.dg1Hash = dg1.getHash();
				} catch (NoSuchAlgorithmException e) {
					return true;
				}
				try {
					BioData.dg2Hash = dg2.getHash();
				} catch (NoSuchAlgorithmException e) {
					return true;
				}
			} catch (Exception e) {
				return true; // catches any exception in the3 method not caught
								// elsewhere
			}
			return false;

		}

	}
}