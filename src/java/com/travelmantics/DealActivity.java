package com.travelmantics;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private static final int PICTURE_RESULT = 42;
    AppCompatEditText txtTitle;
    AppCompatEditText txtPrice;
    AppCompatEditText txtDescription;
    TravelDeal deal;
    Button button;
    ImageView imageView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseUtil.openFbReference("traveldeals",FirebaseUtil.caller);
        firebaseDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        button = findViewById(R.id.button);
        imageView = findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("inage/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent,"Insert picture"),PICTURE_RESULT);
            }
        });
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal)intent.getSerializableExtra("deal");
        if (deal == null){
            deal = new TravelDeal();
        }
        this.deal=deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
    }

    public void showImage(String url){
        if(url != null && url.isEmpty() == true){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width, width*2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            final StorageReference ref = FirebaseUtil.storageReference.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>(){
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String url = ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {


                        }
                    }).toString();
                    String pictureName = taskSnapshot.getStorage().getPath();
                    deal.setImageUrl(url);
                    showImage(url);

                }

            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        enableEditTexts(FirebaseUtil.isAdmin);
        if(FirebaseUtil.isAdmin == true){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
        }
        else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
        }
        return true;
    }

    private void enableEditTexts(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this,"Deal saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"Deal deleted",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveDeal(){
        deal.setTitle( txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());

        if(deal.getId() == null){
            databaseReference.push().setValue(deal);
        }
        else{
            databaseReference.child(deal.getId()).setValue(deal);
        }
    }


    public void backToList(){
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }

    public void deleteDeal(){
        if(deal == null){
            Toast.makeText(this,"Save deal before attemting to delete",Toast.LENGTH_SHORT).show();
            return;
        }
        databaseReference.child(deal.getId()).removeValue();
        if(deal.getImageName() != null && deal.getImageName().isEmpty() == true){
            StorageReference picRef = FirebaseUtil.firebaseStorage.getReference().child(deal.getImageName());
            picRef.delete();
        }
    }
    public void clean(){
        txtTitle.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        txtTitle.requestFocus();

    }
}
