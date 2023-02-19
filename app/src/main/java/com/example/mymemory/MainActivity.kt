package com.example.mymemory

import android.animation.ArgbEvaluator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryCard
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.utils.DEFAULT_ICONS
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.google.android.material.snackbar.Snackbar
import java.text.FieldPosition

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG="MainActivity"
        private const val CREATE_REQUEST_CODE=560064
    }


    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private var boardSize: BoardSize=BoardSize.EASY // by default boardsize is set to easy
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot=findViewById(R.id.clRoot)
        rvBoard=findViewById(R.id.rvBoard)
        tvNumMoves=findViewById(R.id.tvNumMoves)
        tvNumPairs=findViewById(R.id.tvNumPairs)

        // improving the efficiency
        /*val intent=Intent(this,CreateActivity::class.java)
        intent.putExtra(EXTRA_BOARD_SIZE,BoardSize.MEDIUM)
        startActivity(intent)*/

        setupBoard()
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    // notifying the user has tapped on the particular menu item
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.mi_refresh->{
                if(memoryGame.getNumMoves()>0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game?",null,View.OnClickListener{
                        setupBoard()
                    })
                }else{
                    // setup the game again
                    setupBoard()
                }
            }
            R.id.mi_new_size->{
                showNewSizeDialog()
                return true
            }
            /*R.id.mi_custom->{ // for selecting own pics and adding it to your own game
                showCreationDialog()
                return true
            }*/
        }
        return super.onOptionsItemSelected(item)
    }

    /*private fun showCreationDialog() {
        // select the sizes
        val boardSizeView=LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        // select the radio button chosen for the game
        val radioGroupSize=boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView,View.OnClickListener {
            // Set new value for the board size
            val desiredBoardSize=when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy-> BoardSize.EASY
                R.id.rbMedium-> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            // Navigate to a new screen/activity
            val intent=Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
    }*/

    private fun showNewSizeDialog() {
        // select the sizes
        val boardSizeView=LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        // select the radio button chosen for the game
        val radioGroupSize=boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY->radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM->radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD->radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose New Size",boardSizeView,View.OnClickListener {
            // Set new value for the board size
            boardSize=when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy-> BoardSize.EASY
                R.id.rbMedium-> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String,view: View?,positiveClickListner: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)  // title to be displayed
            .setView(view)  // view to be displayed
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK"){ _,_ ->
                positiveClickListner.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        when(boardSize){
            BoardSize.EASY->{
                tvNumMoves.text="Easy: 4x2"
                tvNumPairs.text="Pairs:0/4"
            }
            BoardSize.MEDIUM->{
                tvNumMoves.text="Medium: 6x3"
                tvNumPairs.text="Pairs:0/9"
            }
            BoardSize.HARD->{
                tvNumMoves.text="Hard: 6x6"
                tvNumPairs.text="Pairs:0/12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame=MemoryGame(boardSize)
        adapter=MemoryBoardAdapter(this,boardSize,memoryGame.cards,object: MemoryBoardAdapter.CardClickListner{
            override fun onCardClicked(position: Int) {
                //Log.i(TAG,"Card clicked $position")
                updateGameWithFlip(position)
            }
        }) // adapter provides a binding for the dataset to the views of the RecyclerView, second parameter represents the number of elements in the grid
        rvBoard.adapter= adapter
        rvBoard.setHasFixedSize(true) // by setting this we ensure that RecyclerViews size is not affected by adapter contents

        rvBoard.layoutManager=GridLayoutManager(this,boardSize.getWidth()) // Layout manager is used to measure and position item views, spancount indicates the number of columns
    }

    private fun updateGameWithFlip(position: Int){
        // Error checking
        if(memoryGame.haveWonGame()){ // alert the user of an invalid move
            Snackbar.make(clRoot,"You already Won!",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){ // alert the user of an invalid move
            Snackbar.make(clRoot,"Invalid Move!!",Snackbar.LENGTH_SHORT).show()
            return
        }
        // Actually flip over the card
        if(memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match! Num pairs found: ${memoryGame.numPairsFound}")

            // add color interpolation to indicate the progress of pairs chosen correctly i.e., similar to progress bar
            var color=ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs(),
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this,R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)

            // Highlighting the number of pairs correctly choosen
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, "You Won!! Congrats", Snackbar.LENGTH_LONG).show()
            }
        }
        // Updating the number of moves the player has made
        tvNumMoves.text="Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged() // notifies the card flipped
    }
}