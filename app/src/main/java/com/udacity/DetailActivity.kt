package com.udacity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityDetailBinding
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        setSupportActionBar(binding.toolbar)

        intent.extras?.run {
            val downloadedTitle = getString(DOWNLOADED_TITLE, "")
            val downloadedFilepath = getString(DOWNLOADED_FILEPATH, "")

            binding.contentDetail.run {
                filename.text = downloadedTitle
                status.text = "Success"
                okButton.setOnClickListener {
                    finish()
                }
            }

        }
    }

    companion object {
        const val DOWNLOADED_FILEPATH = "downloaded_filepath"
        const val DOWNLOADED_TITLE = "downloaded_title"
        const val DOWNLOAD_SUCCESS = "download_success"
    }
}
