package com.example.ride;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.executor.TaskExecutor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OtpVerficationActivity extends AppCompatActivity {

    private String verificationId;
    private FirebaseAuth fAuth;
    ProgressBar progressBar;
    String email,pass;
    FirebaseFirestore firestore;
    private TextView resendCode;
    private StorageReference storageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("OTP Verification");
        setContentView(R.layout.activity_otp_verfication);

        String phoneNumber = getIntent().getStringExtra("number");
        email = getIntent().getStringExtra("email");
        pass = getIntent().getStringExtra("pass");
        storageReference = FirebaseStorage.getInstance().getReference();

        EditText code = findViewById(R.id.code);
        Button confirmBtn = findViewById(R.id.confirmBtn);
        progressBar = findViewById(R.id.progressBar);
        resendCode = findViewById(R.id.resendCode);


        sendVerificationCode(phoneNumber);

        resendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                code.setText("");
                Toast.makeText(OtpVerficationActivity.this, "Resend Code", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                sendVerificationCode(phoneNumber);

            }
        });




        fAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String c = code.getText().toString().trim();
                if(c.isEmpty()||c.length()<6)
                {
                    code.setError("Enter code!");
                    code.requestFocus();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                verifyCode(c);


            }
        });
        

    }

    private void verifyCode(String code)
    {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,code);
        signInWithCredential(credential);

    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        fAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful())
                        {

                            fAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    if(task.isSuccessful())
                                    {

                                        FirebaseUser fuser = fAuth.getCurrentUser();

                                        fuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {

                                                Toast.makeText(OtpVerficationActivity.this, "Verification mail has been sent!!!", Toast.LENGTH_SHORT).show();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                                Toast.makeText(OtpVerficationActivity.this, "Error:"+e.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });

                                        String userId = fAuth.getCurrentUser().getEmail();


                                        DocumentReference documentReference = firestore.collection("users").document(userId);
                                        Map<String,Object> user = new HashMap<>();

                                        user.put("name",getIntent().getStringExtra("name"));
                                        user.put("email",getIntent().getStringExtra("email"));
                                        user.put("phone",getIntent().getStringExtra("number"));
                                        user.put("nid",getIntent().getStringExtra("nid"));
                                        user.put("type",getIntent().getStringExtra("type"));
                                        user.put("dob",getIntent().getStringExtra("dob"));
                                        user.put("addr",getIntent().getStringExtra("addr"));




                                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {

                                                fAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<AuthResult> task) {

                                                            byte bb[] = getIntent().getByteArrayExtra("img");

                                                            if(getIntent().getStringExtra("type").equals("Passenger"))
                                                            {
                                                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("VerifyNidLicense").child("Passenger").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                                                                 StorageReference sr = storageReference.child("NID").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                                    sr.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                        @Override
                                                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                                                            sr.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                                @Override
                                                                                public void onSuccess(Uri uri) {

                                                                                    Map info = new HashMap();
                                                                                    info.put("nidImg",uri.toString());
                                                                                    info.put("verify",false);
                                                                                    info.put("nid",getIntent().getStringExtra("nid"));
                                                                                    info.put("dob",getIntent().getStringExtra("dob"));
                                                                                    databaseReference.updateChildren(info);

                                                                                    Toast.makeText(OtpVerficationActivity.this,"Data Saved & create user",Toast.LENGTH_SHORT).show();
                                                                                    progressBar.setVisibility(View.GONE);
                                                                                    startActivity(new Intent(OtpVerficationActivity.this,CustomerPersonalInfo2Activity.class));
                                                                                    finish();
                                                                                }
                                                                            });



                                                                        }
                                                                    }).addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            Toast.makeText(OtpVerficationActivity.this,"Error:"+e.getMessage(),Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });


                                                            }
                                                            if(getIntent().getStringExtra("type").equals("Driver"))
                                                            {
                                                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("VerifyNidLicense").child("Driver").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                                                               StorageReference sr = storageReference.child("LICENSE").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                                sr.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                    @Override
                                                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                                                        sr.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                            @Override
                                                                            public void onSuccess(Uri uri) {

                                                                                Map info = new HashMap();
                                                                                info.put("licenseImg",uri.toString());
                                                                                info.put("verify",false);
                                                                                info.put("nid",getIntent().getStringExtra("nid"));
                                                                                info.put("dob",getIntent().getStringExtra("dob"));
                                                                                databaseReference.updateChildren(info);

                                                                                Toast.makeText(OtpVerficationActivity.this,"Data Saved & create user",Toast.LENGTH_SHORT).show();
                                                                                progressBar.setVisibility(View.GONE);
                                                                                startActivity(new Intent(OtpVerficationActivity.this,DriverProfile2Activity.class));
                                                                                finish();

                                                                            }
                                                                        });



                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Toast.makeText(OtpVerficationActivity.this,"Error:"+e.getMessage(),Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });










                                                            }

                                                    }
                                                });

                                            }
                                        });

                                    }

                                }
                            });





                        }
                        else
                        {
                            Toast.makeText(OtpVerficationActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
//    private void check_user() {
//
//        FirebaseFirestore fstore = FirebaseFirestore.getInstance();
//        DocumentReference documentReference = fstore.collection("users").document(fAuth.getCurrentUser().getEmail());
//        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                if (documentSnapshot.exists()) {
//
//
//                    if (documentSnapshot.getString("type").equals("Traveller")) {
//                        startActivity(new Intent(OtpVerficationActivity.this,CustomerMapActivity.class));
//                       finish();
//                    } else if (documentSnapshot.getString("type").equals("Driver")) {
//
//                        Intent intent = new Intent(OtpVerficationActivity.this, DriverMapsActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
//                    }
//                }
//            }
//        });
//
//    }

    private void sendVerificationCode(String number)
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,
                60,
                TimeUnit.SECONDS,
                this,
                mCallBack
        );
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks
            mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            verificationId = s;

        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            String code = phoneAuthCredential.getSmsCode();
            if(code!=null)
            {
                progressBar.setVisibility(View.VISIBLE);
//                verifyCode(code);

            }

        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {

            Toast.makeText(OtpVerficationActivity.this, ""+e, Toast.LENGTH_SHORT).show();
        }
    };

}