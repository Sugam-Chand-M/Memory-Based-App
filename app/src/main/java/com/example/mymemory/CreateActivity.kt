package com.example.mymemory

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.utils.BitmapScaler
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.example.mymemory.utils.isPermissionGranted
import com.example.mymemory.utils.requestPermission
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val TAG="CreateActivity"
        private const val PICK_PHOTO_CODE=655
        private const val READ_EXTERNAL_PHOTOS_CODE=560064
        private const val READ_PHOTOS_PERMISSION=android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH=3
        private const val MAX_GAME_NAME_LENGTH=14
    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave:Button

    private lateinit var boardSize: BoardSize
    private var numImagesRequired=-1
    private val chosenImageUris= mutableListOf<Uri>()  // URI-Uniform Resource Identifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        // for selecting the images
        rvImagePicker=findViewById(R.id.rvImagePicker)
        etGameName=findViewById(R.id.etGameName)
        btnSave=findViewById(R.id.btnSave)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val boardSize=intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired=boardSize.getNumPairs()
        supportActionBar?.title="Choose pics(0/$numImagesRequired)"

        btnSave.setOnClickListener{
            saveDataToFirebase()
        }
        etGameName.filters= arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled=shouldEnableSaveButton()
            }

        })

        // for selecting the images
        adapter=ImagePickerAdapter(this,chosenImageUris,boardSize,object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos() // function used to select the images from the phone by their default app such as gallery
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }

            }
        })
        rvImagePicker.adapter=adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager=GridLayoutManager(this,boardSize.getWidth())

    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode== READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            } else{
                Toast.makeText(this,"In order to create a custom game, you need to provide access to your photos",Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // used to display the photos selcted from the gallery on the app
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode!= PICK_PHOTO_CODE || resultCode!=Activity.RESULT_OK || data==null){
            Log.w(TAG,"Did not get the data from the launched activity, user likely cancelled flow")
            return
        }
        val selectedUri=data.data
        val clipData=data.clipData
        if(clipData!=null){
            Log.i(TAG,"clipData numImages ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem=clipData.getItemAt(i)
                if(chosenImageUris.size<numImagesRequired){
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri!=null){
            Log.i(TAG,"data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title="Choose pics (${chosenImageUris.size}/$numImagesRequired)"
        btnSave.isEnabled=shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        // Check if we should enable the save button or not
        if(chosenImageUris.size!=numImagesRequired){
            return false
        }
        if(etGameName.text.isBlank() || etGameName.text.length< MIN_GAME_NAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent=Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)  // for selecting multiple images in a single go
        startActivityForResult(Intent.createChooser(intent,"Choose Pics"),PICK_PHOTO_CODE)
    }

    private fun saveDataToFirebase() {
        Log.i(TAG,"saveDataToFirebase")
        for((index,photoUri) in chosenImageUris.withIndex()){
            val imageByteArray=getImageByteArray(photoUri)

        }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            val source=ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        Log.i(TAG,"Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap= BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG,"Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream=ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }
}