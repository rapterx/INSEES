package com.example.insees.Fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.insees.BottomSheetDialogDevelopers.AnkitFragment
import com.example.insees.BottomSheetDialogDevelopers.BishalFragment
import com.example.insees.BottomSheetDialogDevelopers.RishiFragment
import com.example.insees.BottomSheetDialogDevelopers.SudipFragment
import com.example.insees.R
import com.example.insees.databinding.FragmentAboutDevelopersBinding
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AboutDevelopersFragment : Fragment() {

    private lateinit var binding: FragmentAboutDevelopersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAboutDevelopersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getImages()

        val bottomSheetSudip = SudipFragment()
        val bottomSheetAnkit = AnkitFragment()
        val bottomSheetRishi = RishiFragment()
        val bottomsheetBishal = BishalFragment()

        binding.btnSudip.setOnClickListener {
            bottomSheetSudip.show(childFragmentManager, "BottomSheetDialog")
        }

        binding.btnAnkit.setOnClickListener {
            bottomSheetAnkit.show(childFragmentManager, "BottomSheetDialog")
        }

        binding.btnRishi.setOnClickListener {
            bottomSheetRishi.show(childFragmentManager, "BottomSheetDialog")
        }

        binding.btnBishal.setOnClickListener {
            bottomsheetBishal.show(childFragmentManager, "BottomSheetDialog")
        }
    }

    private fun getImages() {
        loadImage("images/sudip.jpg", binding.sudipImage, "sudip.jpg")
        loadImage("images/ankit.jpg", binding.ankitImage, "ankit.jpg")
        loadImage("images/rishi.jpg", binding.rishiImage, "rishi.jpg")
        loadImage("images/bishal.jpg", binding.bishalImage, "bishal.jpg")
    }

    private fun loadImage(remotePath: String, imageView: ImageView, localFileName: String) {
        val localFile = File(requireContext().filesDir, localFileName)

        if (localFile.exists()) {
            // Load the image from the local file
            Glide.with(this)
                .load(localFile)
                .placeholder(R.drawable.rounded_corners) // Use a placeholder image
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable disk caching
                .into(imageView)
        } else {
            // Download the image from Firebase Storage and save it locally
            val storageRef = FirebaseStorage.getInstance().reference.child(remotePath)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                if (isAdded) {
                    Glide.with(this)
                        .asBitmap()
                        .load(uri)
                        .placeholder(R.drawable.rounded_corners) // Use a placeholder image
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable disk caching
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                imageView.setImageBitmap(resource)
                                saveImageToLocalFile(resource, localFile)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // Handle cleanup if necessary
                            }
                        })
                }
            }.addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(context, "Failed to load image: $remotePath", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImageToLocalFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save image locally", Toast.LENGTH_SHORT).show()
        }
    }
}
