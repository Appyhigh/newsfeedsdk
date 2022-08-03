package com.appyhigh.newsfeedsdk.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.NewsFeedPageActivity
import com.appyhigh.newsfeedsdk.adapter.FeedCommentAdapter
import com.appyhigh.newsfeedsdk.model.FeedComment
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class NonNativeCommentBottomSheet(
    val comments: ArrayList<FeedComment>,
    private val listener: NewsFeedPageActivity.BlogDetailsFragmentListener
) : BottomSheetDialogFragment() {

    private var commentsAdapter: FeedCommentAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_non_native_comments, container, false)
        setFonts(view)
        val rvAllComments = view.findViewById<RecyclerView>(R.id.rv_all_comments)
        rvAllComments.layoutManager = LinearLayoutManager(requireActivity())
        commentsAdapter = FeedCommentAdapter(comments, "default")
        rvAllComments.adapter = commentsAdapter
        val ivSend: ImageView = view.findViewById(R.id.ivSend)
        ivSend.visibility = View.VISIBLE
        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            dismiss()
        }
        val etComment = view.findViewById<EditText>(R.id.et_comment)
        ivSend.setOnClickListener {
            if (!etComment.text.isNullOrEmpty()) {
                listener.onPostComment(etComment.text.toString())
                etComment.setText("")
            }
        }
        return view
    }

    fun updateComments(comment: FeedComment) {
        comments.add(comment)
        commentsAdapter?.notifyItemInserted(comments.size - 1)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.onDismissComment()
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(view?.findViewById(R.id.title))
        Card.setFontFamily(view?.findViewById(R.id.et_comment), true)
    }
}
