package eu.napcode.gonoteit.ui.notes;

import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import eu.napcode.gonoteit.R;
import eu.napcode.gonoteit.data.results.DeletedResult;
import eu.napcode.gonoteit.databinding.FragmentNotesBinding;
import eu.napcode.gonoteit.di.modules.viewmodel.ViewModelFactory;
import eu.napcode.gonoteit.data.results.NotesResult;
import eu.napcode.gonoteit.model.note.NoteModel;
import eu.napcode.gonoteit.repository.Resource;
import eu.napcode.gonoteit.ui.create.CreateActivity;
import eu.napcode.gonoteit.ui.note.NoteActivity;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static eu.napcode.gonoteit.repository.Resource.Status.ERROR;
import static eu.napcode.gonoteit.ui.main.MainActivityProgressBarManager.manageProgressBarDisplaying;
import static eu.napcode.gonoteit.ui.note.NoteActivity.NOTE_ID_KEY;
import static eu.napcode.gonoteit.utils.RevealActivityHelper.REVEAL_X_KEY;
import static eu.napcode.gonoteit.utils.RevealActivityHelper.REVEAL_Y_KEY;

public class NotesFragment extends Fragment implements NotesAdapter.NoteListener {

    @Inject
    ViewModelFactory viewModelFactory;

    private FragmentNotesBinding binding;

    private NotesViewModel viewModel;
    private NotesAdapter notesAdapter;

    boolean recyclerViewLoadAnimationDisplayed;

    private static final int LANDSCAPE_COLUMNS = 2;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        AndroidSupportInjection.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notes, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.viewModel = ViewModelProviders
                .of(this, this.viewModelFactory)
                .get(NotesViewModel.class);

        setupViews();
        subscribeToNotes();
    }

    private void subscribeToNotes() {
        NotesResult notesResult = this.viewModel.getNotes();
        notesResult.getNotes().observe(this, this::displayNotes);
        notesResult.getResource().observe(this, this::processResource);
    }

    private void displayNotes(PagedList<NoteModel> noteModels) {
        notesAdapter.submitList(noteModels);

        if (recyclerViewLoadAnimationDisplayed == false) {
            recyclerViewLoadAnimationDisplayed = true;
            this.binding.recyclerView.scheduleLayoutAnimation();
        }
    }

    private void processResource(Resource resource) {
       manageProgressBarDisplaying(getActivity(), resource.status);

        if (resource.status == ERROR) {
            displayMessage(resource.message);
        }
    }

    private void displayMessage(String message) {
        Snackbar.make(binding.constraintLayout, message, Snackbar.LENGTH_LONG).show();
    }

    private void setupViews() {
        setupRecyclerView();

        this.binding.createFab.setOnClickListener(v -> displayCreateActivity());
    }

    private void displayCreateActivity() {
        Intent intent = new Intent(NotesFragment.this.getContext(), CreateActivity.class);
        intent.putExtra(REVEAL_X_KEY, getRevealXForCreateActivity());
        intent.putExtra(REVEAL_Y_KEY, getRevealYForCreateActivity());

        startActivity(intent, getOptionsForCreateActivity());
    }

    private Bundle getOptionsForCreateActivity() {
        String transitionName = getString(R.string.transition_circular_reveal_create_screen);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(getActivity(), binding.createFab, transitionName);

        return options.toBundle();
    }

    private int getRevealXForCreateActivity() {
        return (int) (binding.createFab.getX() + binding.createFab.getWidth() / 2);
    }

    private int getRevealYForCreateActivity() {
        return (int) (binding.createFab.getY() + binding.createFab.getHeight() / 2);
    }

    private void setupRecyclerView() {
        setupLayoutManager();

        this.notesAdapter = new NotesAdapter(getContext(), this);
        this.binding.recyclerView.setAdapter(notesAdapter);

        this.binding.recyclerView.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(getContext(), R.anim.recycler_view_animation)
        );
    }

    private void setupLayoutManager() {

        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            this.binding.recyclerView.setLayoutManager(new GridLayoutManager(getContext(), LANDSCAPE_COLUMNS));
        } else  {
            this.binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        viewModel.getNotes();
    }

    @Override
    public void onDeleteNote(Long id) {
        DeletedResult deletedResult = viewModel.deleteNote(id);
        deletedResult.getResource().observe(this, this::processDeleteResponse);
    }

    @Override
    public void onClickNote(NoteModel noteModel, Pair<View, String>... sharedElementPairs) {
        Intent intent = new Intent(getContext(), NoteActivity.class);
        intent.putExtra(NOTE_ID_KEY, noteModel.getId());

        startActivity(intent, getAnimationBundle(sharedElementPairs));
    }

    @Override
    public void onNoteListChanged() {
        ((LinearLayoutManager)binding.recyclerView.getLayoutManager())
                .scrollToPositionWithOffset(0, 0);
    }

    private Bundle getAnimationBundle(Pair<View, String>... sharedElementPairs) {
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                .makeSceneTransitionAnimation(getActivity(), sharedElementPairs);

        return optionsCompat.toBundle();
    }

    private void processDeleteResponse(Resource<Boolean> booleanResource) {
        manageProgressBarDisplaying(getActivity(), booleanResource.status);

        if (booleanResource.status == ERROR) {
            displayMessage(booleanResource.message);
        }
    }
}
