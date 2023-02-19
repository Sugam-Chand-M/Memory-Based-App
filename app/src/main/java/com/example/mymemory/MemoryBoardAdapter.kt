package com.example.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryCard
import java.lang.Integer.min

// Viewholder provides view of all in a single RecyclerView
class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListner: CardClickListner
) : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object { // companions are singletons where we define constants
        private const val MARGIN_SIZE=10
        private const val TAG="MemoryBoardAdapter"
    }

    interface CardClickListner{ // to change the state on clicking the card we define this interface
        fun onCardClicked(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // responsible for taking the data which is at position and binding it to the ViewHolder in the below function and create one view of the recycler view
        val cardWidth=parent.width/boardSize.getWidth() -(2* MARGIN_SIZE)
        val cardHeight=parent.height/boardSize.getHeight() - (2* MARGIN_SIZE)
        val cardSideLength=min(cardWidth,cardHeight)
        val view=LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)
        val layoutParams= view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width=cardSideLength
        layoutParams.height=cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // responsible for taking data at position and binding with the viewholder
        holder.bind(position)
    }

    override fun getItemCount()= boardSize.numCards

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val imageButton=itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val memoryCard=cards[position]
            imageButton.setImageResource(if(memoryCard.isFaceUp) memoryCard.identifier else R.drawable.bamboo)
            imageButton.alpha=if(memoryCard.isMatched) .4f else 1.0f

            val colorStateList=if(memoryCard.isMatched) ContextCompat.getColorStateList(context,R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)

            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked on position $position")
                cardClickListner.onCardClicked(position)
            }
        }
    }
}
