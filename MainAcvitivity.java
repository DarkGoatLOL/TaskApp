package com.example.domninegor;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Update;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Entity(tableName = "tasks")
    public static class Task {
        @PrimaryKey(autoGenerate = true)
        public int id;
        @ColumnInfo(name = "title")
        public String title;
        @ColumnInfo(name = "description")
        public String description;
        @ColumnInfo(name = "is_completed")
        public boolean isCompleted;

        public Task() {
        }

        public Task(String title, String description, boolean isCompleted) {
            this.title = title;
            this.description = description;
            this.isCompleted = isCompleted;
        }
    }

    @Dao
    public interface TaskDao {
        @Query("SELECT * FROM tasks ORDER BY id DESC")
        LiveData<List<Task>> getAllTasks();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Task task);

        @Update
        void update(Task task);

        @Delete
        void delete(Task task);
    }

    @Database(entities = {Task.class}, version = 1, exportSchema = false)
    public abstract static class AppDatabase extends RoomDatabase {
        public abstract TaskDao taskDao();

        private static volatile AppDatabase instance;
        private static final Object LOCK = new Object();

        public static AppDatabase getInstance(Context context) {
            if (instance == null) {
                synchronized (LOCK) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "tasks_database")
                                .build();
                    }
                }
            }
            return instance;
        }
    }

    public static class TaskViewModel extends ViewModel {
        private final TaskDao taskDao;
        private final LiveData<List<Task>> allTasks;

        public TaskViewModel(Context context) {
            AppDatabase db = AppDatabase.getInstance(context);
            taskDao = db.taskDao();
            allTasks = taskDao.getAllTasks();
        }

        public LiveData<List<Task>> getAllTasks() {
            return allTasks;
        }
        public void addTask(Task task) {
            Executors.newSingleThreadExecutor().execute(() -> taskDao.insert(task));
        }
        public void updateTask(Task task) {
            Executors.newSingleThreadExecutor().execute(() -> taskDao.update(task));
        }
        public void deleteTask(Task task) {
            Executors.newSingleThreadExecutor().execute(() -> taskDao.delete(task));
        }
        public void toggleTaskCompletion(Task task) {
            task.isCompleted = !task.isCompleted;
            updateTask(task);
        }
    }

    public static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

        private List<Task> tasks = new ArrayList<>();
        private final OnTaskClickListener onTaskCheckListener;
        private final OnTaskLongClickListener onTaskLongClickListener;

        public interface OnTaskClickListener {
            void onTaskChecked(Task task, boolean isChecked);
        }

        public interface OnTaskLongClickListener {
            void onTaskLongClick(Task task);
        }

        public TaskAdapter(OnTaskClickListener checkListener, OnTaskLongClickListener longClickListener) {
            this.onTaskCheckListener = checkListener;
            this.onTaskLongClickListener = longClickListener;
        }

        public void setTasks(List<Task> newTasks) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return tasks.size();
                }

                @Override
                public int getNewListSize() {
                    return newTasks.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return tasks.get(oldItemPosition).id == newTasks.get(newItemPosition).id;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Task oldTask = tasks.get(oldItemPosition);
                    Task newTask = newTasks.get(newItemPosition);
                    return oldTask.title.equals(newTask.title) && oldTask.description.equals(newTask.description) && oldTask.isCompleted == newTask.isCompleted;
                }
            });
            tasks = newTasks;
            diffResult.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout itemLayout = new LinearLayout(parent.getContext());
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 16, 16, 16);
            itemLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView titleView = new TextView(parent.getContext());
            titleView.setTextSize(18);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            textContainer.addView(titleView);

            TextView descView = new TextView(parent.getContext());
            descView.setTextSize(14);
            descView.setVisibility(View.GONE);
            textContainer.addView(descView);

            CheckBox checkBox = new CheckBox(parent.getContext());
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            ImageButton deleteButton = new ImageButton(parent.getContext());
            deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
            deleteButton.setBackground(null);
            deleteButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            itemLayout.addView(textContainer);
            itemLayout.addView(checkBox);
            itemLayout.addView(deleteButton);

            return new TaskViewHolder(itemLayout, titleView, descView, checkBox, deleteButton);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = tasks.get(position);
            holder.titleView.setText(task.title);
            if (task.description != null && !task.description.isEmpty()) {
                holder.descView.setText(task.description);
                holder.descView.setVisibility(View.VISIBLE);
            } else {
                holder.descView.setVisibility(View.GONE);
            }

            holder.checkBox.setChecked(task.isCompleted);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                onTaskCheckListener.onTaskChecked(task, isChecked);
            });

            holder.deleteButton.setOnClickListener(v -> onTaskLongClickListener.onTaskLongClick(task));
            float alpha = task.isCompleted ? 0.5f : 1.0f;
            holder.itemView.setAlpha(alpha);
            holder.itemView.setOnLongClickListener(v -> {
                onTaskLongClickListener.onTaskLongClick(task);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }
        public static class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView titleView, descView;
            CheckBox checkBox;
            ImageButton deleteButton;
            public TaskViewHolder(@NonNull View itemView, TextView title, TextView desc, CheckBox cb, ImageButton del) {
                super(itemView);
                titleView = title;
                descView = desc;
                checkBox = cb;
                deleteButton = del;
            }
        }
    }

    private TextView totalTasksTextView, completedTasksTextView, dateTextView;
    private Button categoryButton;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private String currentCategory = "Все";
    private List<Task> originalTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalTasksTextView = findViewById(R.id.totalTasksTextView);
        completedTasksTextView = findViewById(R.id.completedTasksTextView);
        dateTextView = findViewById(R.id.dateTextView);
        categoryButton = findViewById(R.id.categoryButton);
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab);

        String currentDate = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
        dateTextView.setText(currentDate);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TaskViewModel(getApplicationContext());
            }
        }).get(TaskViewModel.class);

        adapter = new TaskAdapter(
                (task, isChecked) -> {
                    task.isCompleted = isChecked;
                    viewModel.updateTask(task);
                },
                task -> {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Удаление задачи").setMessage("Вы уверены, что хотите удалить задачу \"" + task.title + "\"?").setPositiveButton("Удалить", (dialog, which) -> viewModel.deleteTask(task)).setNegativeButton("Отмена", null).show();
                }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        viewModel.getAllTasks().observe(this, tasks -> {
            originalTasks = tasks;
            updateFilteredList();
            updateStatistics();
        });

        fab.setOnClickListener(v -> showAddTaskDialog());
        categoryButton.setOnClickListener(v -> {
            switch (currentCategory) {
                case "Все":
                    currentCategory = "Активные";
                    categoryButton.setText("Активные");
                    break;
                case "Активные":
                    currentCategory = "Выполненные";
                    categoryButton.setText("Выполненные");
                    break;
                default:
                    currentCategory = "Все";
                    categoryButton.setText("Все");
                    break;
            }
            updateFilteredList();
        });
    }

    private void updateFilteredList() {
        List<Task> filtered = new ArrayList<>();
        for (Task task : originalTasks) {
            switch (currentCategory) {
                case "Активные":
                    if (!task.isCompleted) filtered.add(task);
                    break;
                case "Выполненные":
                    if (task.isCompleted) filtered.add(task);
                    break;
                default:
                    filtered.add(task);
                    break;
            }
        }
        adapter.setTasks(filtered);
    }

    private void updateStatistics() {
        int total = originalTasks.size();
        int completed = 0;
        for (Task task : originalTasks) {
            if (task.isCompleted) completed++;
        }
        totalTasksTextView.setText("Всего: " + total);
        completedTasksTextView.setText("Выполнено: " + completed);
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить задачу");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        final android.widget.EditText titleInput = new android.widget.EditText(this);
        titleInput.setHint("Название");
        layout.addView(titleInput);
        final android.widget.EditText descInput = new android.widget.EditText(this);
        descInput.setHint("Описание");
        layout.addView(descInput);
        builder.setView(layout);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
                return;
            }
            Task newTask = new Task(title, desc, false);
            viewModel.addTask(newTask);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}
