package com.example.insees.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.insees.Adapters.HomeToDoAdapter
import com.example.insees.Dataclasses.ToDoData
import com.example.insees.R
import com.example.insees.Utils.DialogAddBtnClickListener
import com.example.insees.Utils.FirebaseManager
import com.example.insees.Utils.HomeViewModel
import com.example.insees.Utils.Swipe
import com.example.insees.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), DialogAddBtnClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var navController: NavController
    private lateinit var databaseRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var popUpFragment: PopUpFragment
    private lateinit var homeAdapter: HomeToDoAdapter
    private var tasks: MutableList<ToDoData> = mutableListOf()
    private lateinit var viewModel: HomeViewModel
    private var isDataLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        viewPager = requireActivity().findViewById(R.id.viewPager)

        setUpViews()

        viewModel.userData.observe(viewLifecycleOwner) {
            binding.tvHello.text = it
        }

        registerEvents()
        initSwipe()

        fetchDatabase()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.fetchUserData()

        binding.cardViewStudyMaterials.setOnClickListener {
            viewPager.currentItem = 1
        }

        binding.btnProfile.setOnClickListener {
            navController.navigate(R.id.action_viewPagerFragment_to_profileFragment)
        }

        binding.cardViewInsees.setOnClickListener {
            viewPager.currentItem = 3
        }

        binding.cardViewMembers.setOnClickListener {
            viewPager.currentItem = 3
        }

        binding.btnViewAll.setOnClickListener {
            viewPager.currentItem = 2
        }

        binding.btnAddTask.setOnClickListener {
            navController.navigate(R.id.action_viewPagerFragment_to_popUpFragment)
        }
        return binding.root
    }

    private fun setUpViews() {
        binding.rvTodo.layoutManager = LinearLayoutManager(context)
        homeAdapter = HomeToDoAdapter(tasks)
        binding.rvTodo.adapter = homeAdapter
        updateRecyclerViewVisibility()
    }

    private fun updateRecyclerViewVisibility() {
        if (tasks.isEmpty()) {
            binding.rvTodo.visibility = View.GONE
        } else {
            binding.rvTodo.visibility = View.VISIBLE
        }
    }

    private fun registerEvents() {
        binding.btnAddTask.setOnClickListener {
            popUpFragment = PopUpFragment()
            popUpFragment.setListener(this)
            popUpFragment.show(childFragmentManager, "PopUpFragment")
        }
    }

    private fun fetchDatabase() {
        isDataLoaded = true

        databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.keepSynced(true)
        currentUser = FirebaseManager.getFirebaseAuth().currentUser!!

        lifecycleScope.launch {
            val query = databaseRef.child("users").child(currentUser.uid).child("Tasks")
            query.addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    tasks.clear()
                    for (taskSnapshot in snapshot.children) {
                        val taskTitle = taskSnapshot.child("title").getValue(String::class.java) ?: ""
                        val taskDesc = taskSnapshot.child("description").getValue(String::class.java) ?: ""
                        val taskTime = taskSnapshot.child("time").getValue(String::class.java) ?: ""
                        val taskDate = taskSnapshot.child("date").getValue(String::class.java) ?: ""

                        val todoTask = ToDoData(taskTitle, taskDesc, taskTime, taskDate)
                        tasks.add(todoTask)
                    }
                    tasks.sortWith(compareBy({ it.taskDate }, { it.taskTime }))

                    updateRecyclerViewVisibility()
                    homeAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error in Fetching data", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onSaveTask(
        todoTitle: String,
        todoTitleEt: EditText,
        todoDesc: String,
        todoDescEt: EditText,
        todoTime: String,
        todoTimeEt: TextView,
        todoDate: String,
        todoDateEt: TextView
    ) {
        val task = hashMapOf(
            "title" to todoTitle,
            "description" to todoDesc,
            "time" to todoTime,
            "date" to todoDate
        )
        val database = databaseRef
            .child("users")
            .child(currentUser.uid)
            .child("Tasks")

        database.push().setValue(task).addOnCompleteListener { tasks ->
            if (tasks.isSuccessful) {
                Toast.makeText(context, "Todo Saved Successfully", Toast.LENGTH_SHORT).show()
                todoTitleEt.text = null
                todoDescEt.text = null
                todoDateEt.text = null
                todoTimeEt.text = null
            } else {
                Toast.makeText(context, tasks.exception.toString(), Toast.LENGTH_SHORT).show()
            }
            popUpFragment.dismiss()
        }
        updateRecyclerViewVisibility()
        homeAdapter.notifyDataSetChanged()
    }

    private fun initSwipe() {
        val swipe = object : Swipe() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = homeAdapter.getItem(position)
                if (direction == ItemTouchHelper.LEFT) {
                    lifecycleScope.launch {
                        onSwiped(task)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Task Deleted", Toast.LENGTH_SHORT).show()
                            homeAdapter.notifyDataSetChanged()
                        }
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    lifecycleScope.launch {
                        onSwiped(task)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Task Finished", Toast.LENGTH_SHORT).show()
                            homeAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipe)
        itemTouchHelper.attachToRecyclerView(binding.rvTodo)
    }

    private fun onSwiped(toDoData: ToDoData) {
        val database = databaseRef
            .child("users")
            .child(currentUser.uid)
            .child("Tasks")

        database
            .orderByChild("title")
            .equalTo(toDoData.taskTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        // Check if the found entry matches the data to be deleted
                        if (taskSnapshot.child("title").getValue(String::class.java) == toDoData.taskTitle &&
                            taskSnapshot.child("description").getValue(String::class.java) == toDoData.taskDesc &&
                            taskSnapshot.child("time").getValue(String::class.java) == toDoData.taskTime &&
                            taskSnapshot.child("date").getValue(String::class.java) == toDoData.taskDate) {
                            // Delete the entry
                            taskSnapshot.ref.removeValue().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}
