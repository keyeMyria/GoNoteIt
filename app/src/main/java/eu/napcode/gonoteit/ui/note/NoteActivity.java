package eu.napcode.gonoteit.ui.note;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import eu.napcode.gonoteit.R;
import eu.napcode.gonoteit.databinding.ActivityNoteBinding;
import eu.napcode.gonoteit.di.modules.viewmodel.ViewModelFactory;
import eu.napcode.gonoteit.model.note.NoteModel;
import eu.napcode.gonoteit.data.results.NoteResult;
import eu.napcode.gonoteit.repository.Resource;
import eu.napcode.gonoteit.ui.create.CreateActivity;
import eu.napcode.gonoteit.utils.GlideBase64Loader;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static eu.napcode.gonoteit.repository.Resource.Status.ERROR;
import static eu.napcode.gonoteit.repository.Resource.Status.LOADING;
import static eu.napcode.gonoteit.ui.create.CreateActivity.EDIT_NOTE_ID_KEY;

public class NoteActivity extends AppCompatActivity {

    @Inject
    ViewModelFactory viewModelFactory;

    @Inject
    GlideBase64Loader glideBase64Loader;

    ActivityNoteBinding binding;
    private NoteViewModel viewModel;

    public static final String NOTE_ID_KEY = "note id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_note);

        AndroidInjection.inject(this);

        setupViewModel();
        getNote();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.update) {
            showUpdateScreen();
        }

        return super.onOptionsItemSelected(item);
    }

    void showUpdateScreen() {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this);

        Intent intent = new Intent(this, CreateActivity.class);
        intent.putExtras(getBundleForUpdate());

        startActivity(intent, options.toBundle());
    }

    private Bundle getBundleForUpdate() {
        Bundle bundle = new Bundle();
        bundle.putLong(EDIT_NOTE_ID_KEY, getIntent().getLongExtra(NOTE_ID_KEY, 0));

        return bundle;
    }

    void setupViewModel() {
        this.viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NoteViewModel.class);
    }

    private void getNote() {
        Long id = getIntent().getLongExtra(NOTE_ID_KEY, 0);
        NoteResult noteResult = this.viewModel.getNote(id);

        noteResult.getNote().observe(this, this::displayNote);
        noteResult.getResource().observe(this, this::processNote);
    }

    private void processNote(Resource resource) {
        boolean loading = resource.status == LOADING;
        binding.progressBar.setVisibility(loading ? VISIBLE : GONE);

        if (resource.status == ERROR) {
            showError(resource.message);
        }
    }

    private void showError(String message) {

        if (message == null) {
            message = getString(R.string.error_with_saving_note);
        }

        Snackbar.make(binding.constraintLayout, message, LENGTH_LONG).show();
    }


    private void displayNote(NoteModel noteModel) {
        displayTextInTextView(noteModel.getTitle(), binding.noteTitleTextView);
        displayTextInTextView(noteModel.getContent(), binding.noteTextView);

        if (!TextUtils.isEmpty(noteModel.getImageBase64())) {
            displayImage(noteModel);
        } else {
            binding.imageView.setVisibility(GONE);
        }
    }

    private void displayTextInTextView(String text, TextView textView) {

        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(VISIBLE);
        }
    }

    private void displayImage(NoteModel noteModel) {
        glideBase64Loader.loadBase64IntoView(noteModel.getImageBase64(), binding.imageView);

        binding.imageView.setVisibility(VISIBLE);
    }
}
