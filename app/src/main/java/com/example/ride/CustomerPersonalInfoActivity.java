package com.example.ride;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.ride.databinding.ActivityCustomerMapBinding;
import com.example.ride.databinding.ActivityCustomerPersonalInfoBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerPersonalInfoActivity extends MainActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore fstore;
    ActivityCustomerPersonalInfoBinding binding;
    EditText name;
    TextView mobile;
    Button save;
    DatabaseReference databaseReference;
    String userId;
    String cName,cMobile,cPhone,profileImgUrl;
    CircleImageView imageView;
    private Uri resUri;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerPersonalInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();
        name = findViewById(R.id.customerProfileName);
        mobile = findViewById(R.id.customerMobileNo);
        save = findViewById(R.id.saveBtn);
        imageView = findViewById(R.id.customerProfileImage);

        userId = firebaseAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);

      // cPhone = getIntent().getStringExtra("mobile");
        getUserInfo2();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = new Dialog(CustomerPersonalInfoActivity.this);

                dialog.setContentView(R.layout.loader);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loader_dialog));
                }
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.setCancelable(false);
                dialog.show();
                saveUserInfo();
            }
        });


    }

    private void saveUserInfo() {
        Map userInfo = new HashMap();
        userInfo.put("name",name.getText().toString());
        userInfo.put("phone",cPhone);
        databaseReference.updateChildren(userInfo);
        if(resUri != null)
        {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);

            storageReference.putFile(resUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Map newImg = new HashMap();
                            newImg.put("profileImageUrl",uri.toString());
                            databaseReference.updateChildren(newImg);
                            Toast.makeText(CustomerPersonalInfoActivity.this,"Data saved",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();

                        }
                    });

                }
            });


//            Bitmap bitmap = null;
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resUri);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            ByteArrayOutputStream bas = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG,20,bas);
//            byte[] data = bas.toByteArray();
//            UploadTask uploadTask = storageReference.putBytes(data);
//
//            uploadTask.addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    finish();
//                    return;
//                }
//            });
//
//            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    Uri downloadUrl = taskSnapshot.getUploadSessionUri();
//                    Map newImg = new HashMap();
//                    newImg.put("profileImageUrl",downloadUrl.toString());
//                    databaseReference.updateChildren(newImg);
//                    finish();
//                    return;
//
//                }
//            });

        }
        else
        {
            finish();
        }


    }

    private void getUserInfo2()
    {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount()>0)
                {
                    Map<String, Object>map = (Map<String, Object>) snapshot.getValue();

                    if(map.get("name")!=null)
                    {
                        cName = map.get("name").toString();
                        name.setText(cName);
                    }
                    if(map.get("phone")!=null)
                    {
                        cMobile = map.get("phone").toString();
                        cPhone = cMobile;
                        mobile.setText("Mobile: "+cMobile);
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                        profileImgUrl = map.get("profileImageUrl").toString();
                        Glide.with(CustomerPersonalInfoActivity.this)
                                .load(profileImgUrl)
                                .into(imageView);

                    }

                }
                else
                {
                    getUserInfo();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getUserInfo()
    {
        DocumentReference documentReference = fstore.collection("users").document(firebaseAuth.getCurrentUser().getEmail());
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    name.setText(documentSnapshot.getString("name"));
                    mobile.setText("Mobile: "+documentSnapshot.getString("phone"));
                    cPhone = documentSnapshot.getString("phone");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == Activity.RESULT_OK)
        {
            final Uri imageUri = data.getData();
            resUri = imageUri;
            imageView.setImageURI(imageUri);
        }

    }
}