package com.duncanwestland.chk;


import com.duncanwestland.chk.R;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.ImageButton;
import android.widget.TextView;

public class PassportSummaryActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary);

        //setup GUI
        ImageButton imageButtonPortrait = (ImageButton) findViewById(R.id.imageButtonPortrait);
        TextView textViewName = (TextView) findViewById(R.id.textViewName);
        TextView textViewDob = (TextView) findViewById(R.id.textViewDob);
        TextView textViewDoe = (TextView) findViewById(R.id.textViewDoe);
        TextView textViewDoc = (TextView) findViewById(R.id.textViewDoc);
        TextView textViewGender = (TextView) findViewById(R.id.textViewGender);        
        textViewName.setText(BioData.name);
        textViewDob.setText((String)DateFormat.format("MMM dd, yyyy", BioData.dateOfBirth));
        textViewDoe.setText((String)DateFormat.format("MMM dd, yyyy", BioData.dateOfExpiry));
        textViewGender.setText(BioData.sex);
        textViewDoc.setText(BioData.passportNumber);
        Bitmap smallPortrait = Bitmap.createScaledBitmap(BioData.portrait, 210, 270, true);
        imageButtonPortrait.setImageBitmap(smallPortrait);   
    }
    @Override
    public void onPause() {
    	super.onPause();
    	//BioData.clear(); //this ensures that personal data isn't retained through a pause
    }
	@Override
	public void onResume() {//TODO check this runs after oncreate!
	    super.onResume();
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	@Override
	public void onStop() {
		super.onStop();
		BioData.clear();
	}
}