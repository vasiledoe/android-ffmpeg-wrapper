package com.floodin.videoeditor.base.view

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem
import com.floodin.videoeditor.base.viewmodel.MainViewModel
import com.floodin.videoeditor.databinding.ActivityMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : BaseMediaActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mVideosAdapter: VideosAdapter
    private val mViewModel by viewModel<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setUpViews()
        setUpEventsAdapter()
        onBindModel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_concat -> {
                mViewModel.concatVideos()
                true
            }
            R.id.action_compress -> {
                mViewModel.compressVideo()
                true
            }
            R.id.action_compress_multiple -> {
                mViewModel.compressMultipleVideos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpViews() {
        mBinding.btnSelectVideos.setOnClickListener {
            if (hasStoragePermission()) {
                selectVideos()
            } else {
                requestStoragePermission()
            }
        }
    }

    override fun onStoragePermissionGranted() {
        selectVideos()
    }

    private fun onBindModel() {
        mViewModel.loadingState.observe(this) {
            mBinding.progressBar.boolVisible(it)
        }
        mViewModel.error.observe(this) {
            showToast(it)
        }
        mViewModel.successMsg.observe(this) {
            showToast(it)
        }
        mViewModel.selectedVideoItems.observe(this) {
            loadMedias(it)
        }
    }

    private val mPickerItemsResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            mViewModel.applySelectedData(data)
        }
    }

    private fun selectVideos() {
        val intent = Intent(ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "video/*"
        mPickerItemsResult.launch(intent)
    }

    /**
     * Set events adapter and forget. Then when items will be selected we'll use this adapter.
     */
    private fun setUpEventsAdapter() {
        mVideosAdapter = VideosAdapter(
            items = mutableListOf(),
            onEventClickedCallback = { openVideo(it.uri) }
        )
        mBinding.rvItems.adapter = mVideosAdapter
        mBinding.rvItems.setHasFixedSize(true)
        mBinding.rvItems.layoutManager = LinearLayoutManager(
            this@MainActivity,
            RecyclerView.VERTICAL,
            false
        )
    }

    private fun loadMedias(items: List<VideoItem>) {
        if (items.isNotEmpty()) {
            mBinding.tvNoVideos.gone()
            mBinding.rvItems.visible()
            mVideosAdapter.updateMedias(items)

        } else {
            mBinding.tvNoVideos.visible()
            mBinding.rvItems.gone()
        }
    }

    private fun openVideo(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "video/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }
}