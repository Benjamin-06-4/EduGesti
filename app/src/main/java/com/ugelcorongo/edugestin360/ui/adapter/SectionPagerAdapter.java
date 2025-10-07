package com.ugelcorongo.edugestin360.ui.adapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.domain.models.Pregunta;

public class SectionPagerAdapter extends FragmentStateAdapter {

    private final List<String> sections;
    private final Map<String, List<Pregunta>> preguntasMap;
    private final ColegioInfo colegioInfo;
    private final Especialista insta_especialista;
    private final String docente_enficha, tipoFicha;

    public SectionPagerAdapter(@NonNull FragmentActivity fa,
                               List<String> sections,
                               Map<String, List<Pregunta>> preguntasMap,
                               ColegioInfo info, Especialista especialista,
                               String tipoFicha, String docenteName) {
        super(fa);
        this.sections = sections;
        this.preguntasMap = preguntasMap;
        this.colegioInfo   = info;
        this.insta_especialista   = especialista;
        this.tipoFicha   = tipoFicha;
        this.docente_enficha   = docenteName;
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        String sec = sections.get(position);
        List<Pregunta> lista = preguntasMap.getOrDefault(sec, Collections.emptyList());
        ArrayList<Pregunta> preguntas = new ArrayList<>(lista);

        boolean isFirst = position == 0;
        boolean isLast  = position == sections.size() - 1;

        // Pasamos isFirst e isLast
        return SectionFragment.newInstance(preguntas, isFirst, isLast, colegioInfo, insta_especialista, tipoFicha, docente_enficha);
    }

    @Override public int getItemCount() {
        return sections.size();
    }
}