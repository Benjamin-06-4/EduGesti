package com.ugelcorongo.edugestin360.ui.adapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.android.material.internal.TextWatcherAdapter;
import com.ugelcorongo.edugestin360.R;
import com.ugelcorongo.edugestin360.domain.models.Especialista;
import com.ugelcorongo.edugestin360.managers.VolleyMultipartRequest;
import com.ugelcorongo.edugestin360.domain.models.ColegioInfo;
import com.ugelcorongo.edugestin360.domain.models.OpcionRespuesta;
import com.ugelcorongo.edugestin360.domain.models.Pregunta;
import com.ugelcorongo.edugestin360.managers.upload.FichaUploadManager;
import com.ugelcorongo.edugestin360.remote.ApiService;
import com.ugelcorongo.edugestin360.repository.DataRepository;
import com.ugelcorongo.edugestin360.ui.viewmodel.EspecialistaViewModel;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;
import com.ugelcorongo.edugestin360.utils.UriUtils;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

public class SectionFragment extends Fragment {

    private static final String ARG_PREGUNTAS     = "argPreguntas";
    private static final String ARG_IS_FIRST     = "argIsFirst";
    private static final String ARG_IS_LAST       = "argIsLast";
    private static final String ARG_COLEGIO_INFO  = "argColegioInfo";
    private static final String ARG_ESPECIALISTA_INFO  = "argEspecialistaInfo";
    private static final String ARG_TIPOFICHA     = "argTipoFicha";
    private static final String ARG_DOCENTE_NAME   = "argDocenteName";
    private String currentPhotoQuestionId;

    private List<Pregunta> preguntas;
    private boolean        isFirst;
    private boolean        isLast;
    private ColegioInfo    colegioInfo;
    private Especialista insta_especialista;
    private String tipoFicha, docente_enficha;

    private LinearLayout   llContainer;
    private EspecialistaViewModel vm;

    private Map<String, Uri> photoUris = new HashMap<>();

    private ActivityResultLauncher<String> cameraPermLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    public static SectionFragment newInstance(
            ArrayList<Pregunta> preguntas,
            boolean isFirst,
            boolean isLast,
            ColegioInfo info,
            Especialista especialista,
            String tipoFicha,
            String docenteName
    ) {
        SectionFragment frag = new SectionFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_PREGUNTAS, preguntas);
        args.putBoolean(ARG_IS_FIRST, isFirst);
        args.putBoolean(ARG_IS_LAST, isLast);
        args.putParcelable(ARG_COLEGIO_INFO, info);
        args.putParcelable(ARG_ESPECIALISTA_INFO, especialista);
        args.putString(ARG_TIPOFICHA, tipoFicha);
        args.putString(ARG_DOCENTE_NAME, docenteName);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(EspecialistaViewModel.class);

        Bundle args = getArguments();
        preguntas    = args.getParcelableArrayList(ARG_PREGUNTAS);
        isFirst     = args.getBoolean(ARG_IS_FIRST, false);
        isLast       = args.getBoolean(ARG_IS_LAST, false);
        colegioInfo  = args.getParcelable(ARG_COLEGIO_INFO);
        insta_especialista  = args.getParcelable(ARG_ESPECIALISTA_INFO);
        tipoFicha  = args.getString(ARG_TIPOFICHA);
        docente_enficha  = args.getString(ARG_DOCENTE_NAME);
        setupActivityResultLaunchers();
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View root = inflater.inflate(
                R.layout.fragment_section_questions,
                container,
                false
        );
        llContainer = root.findViewById(R.id.llQuestionContainer);

        if (colegioInfo == null) {
            // muestra mensaje de error en UI
            TextView tv = new TextView(getContext());
            tv.setText("Error: datos de colegio no disponibles");
            llContainer.addView(tv);
            return root;
        }

        // Solo en la primera sección
        if (isFirst) {
            View header;
            if ("Docente".equalsIgnoreCase(tipoFicha)) {
                header = inflater.inflate(
                        R.layout.header_docente_info,
                        llContainer,
                        false
                );
                ((TextView) header.findViewById(R.id.etNombreIE))
                        .setText(colegioInfo.getNombre());
                ((TextView) header.findViewById(R.id.etCodigoModular))
                        .setText(colegioInfo.getCodigoModular());
                ((TextView) header.findViewById(R.id.etCodigoLocal))
                        .setText(colegioInfo.getCodigoLocal());
                ((TextView) header.findViewById(R.id.etNivel))
                        .setText(colegioInfo.getNivel());
                ((TextView) header.findViewById(R.id.etDirector))
                        .setText(colegioInfo.getDirector());
                EditText etDoc = header.findViewById(R.id.etNombreDocente);
                if (docente_enficha != null) {
                    etDoc.setText(docente_enficha);
                    etDoc.setEnabled(false);
                }
                etDoc.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
                    @Override public void afterTextChanged(Editable s){}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String texto = s.toString().trim();
                        if (listener != null) {
                            listener.onHeaderDocenteChanged(texto);
                        }
                    }
                });

                llContainer.addView(header);
            } else {
                header = inflater.inflate(
                        R.layout.header_colegio_info,
                        llContainer,
                        false
                );
                ((TextView) header.findViewById(R.id.etNombreIE))
                        .setText(colegioInfo.getNombre());
                ((TextView) header.findViewById(R.id.etCodigoModular))
                        .setText(colegioInfo.getCodigoModular());
                ((TextView) header.findViewById(R.id.etCodigoLocal))
                        .setText(colegioInfo.getCodigoLocal());
                ((TextView) header.findViewById(R.id.etNivel))
                        .setText(colegioInfo.getNivel());
                ((TextView) header.findViewById(R.id.etDirector))
                        .setText(colegioInfo.getDirector());
                llContainer.addView(header);
            }

        }

        // Renderizar cada pregunta según su tipo
        for (Pregunta p : preguntas) {
            final String qId = p.getIdPregunta();
            View item = null;

            switch (p.getTipoPregunta()) {
                case "ENUNCIADO":
                    item = inflater.inflate(
                            R.layout.item_enunciado,
                            llContainer,
                            false
                    );
                    TextView tv_enun = item.findViewById(R.id.tvEnunciado);
                    // Insertar salto de línea tras “NIVEL” para legibilidad
                    String texto = p.getTextoPregunta()
                            .replaceFirst(":", ":\n");
                    tv_enun.setText(texto);
                    break;

                case "TEXT":
                    // 1) Inflamos el layout que tiene el label
                    item = inflater.inflate(
                            R.layout.item_question_text,
                            llContainer,
                            false
                    );

                    // 2) Ponemos el enunciado
                    TextView tvLabel_text = item.findViewById(R.id.tvQuestionLabel);
                    tvLabel_text.setText(p.getTextoPregunta());

                    // 3) Creamos o buscamos el EditText para que el usuario escriba
                    //    Asumo que en item_question_text.xml tienes un EditText con id etAnswer
                    EditText etInput = item.findViewById(R.id.etQuestionAnswer);
                    if (etInput == null) {
                        // Si tu layout no lo define, lo creamos dinámico
                        etInput = new EditText(getContext());
                        etInput.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ));
                        // puedes añadir hint si quieres
                        etInput.setHint("Escribe comentario");
                        ((LinearLayout)item).addView(etInput);
                    }

                    // 4) Tag para localizar luego en submitAll()
                    final String textTag = "et_" + qId;
                    etInput.setTag(textTag);

                    // 5) Precargar si ya existe en el ViewModel
                    String savedText = vm.getTextAnswer(qId);
                    if (savedText != null) {
                        etInput.setText(savedText);
                    }

                    // 6) Cada vez que cambie, lo guardamos en el VM
                    etInput.addTextChangedListener(new TextWatcherAdapter() {
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            vm.setTextAnswer(qId, s.toString());
                        }
                    });
                    break;

                case "SINGLE_CHOICE":
                    item = inflater.inflate(R.layout.item_question_single_choice, container, false);
                    TextView tv = item.findViewById(R.id.tvQuestionLabel);
                    tv.setText(p.getTextoPregunta());

                    RadioGroup rg = item.findViewById(R.id.rgQuestionOptions);
                    rg.setTag("rg_" + qId);

                    // Opcional: fijar el weightSum igual al número de opciones
                    int optionCount = p.getOpciones().size();
                    rg.setWeightSum(optionCount);

                    // crea los botones
                    for (OpcionRespuesta opt : p.getOpciones()) {
                        RadioButton rb = new RadioButton(getContext());
                        rb.setId(View.generateViewId());
                        rb.setText(opt.getDescripcion());
                        rb.setSingleLine(false);

                        rb.setMaxLines(2);
                        RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(
                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f );
                        rb.setLayoutParams(lp);

                        rg.addView(rb);
                    }

                    // Precargar selección si existe en VM
                    String saved = vm.getAnswer(qId);
                    if (saved != null) {
                        for (int i = 0; i < rg.getChildCount(); i++) {
                            View c = rg.getChildAt(i);
                            if (c instanceof RadioButton
                                    && ((RadioButton)c).getText().equals(saved)) {
                                rg.check(c.getId());
                                break;
                            }
                        }
                    }

                    // Si requiere Comentario
                    if (p.isRequiereComentario()) {
                        EditText etComment = new EditText(getContext());
                        etComment.setHint("Escribe tu comentario");
                        etComment.setTag("comment_" + qId);

                        // precarga desde VM
                        String savedComment = vm.getComment(qId);
                        if (savedComment != null) {
                            etComment.setText(savedComment);
                        }

                        etComment.addTextChangedListener(new TextWatcherAdapter() {
                            @SuppressLint("RestrictedApi")
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                vm.setComment(qId, s.toString());
                            }
                        });
                        //container.addView(etComment);
                        ((LinearLayout) item).addView(etComment);
                    }

                    // Si requiere Foto
                    // Si requiere foto
                    if (p.isRequiereFoto()) {
                        LinearLayout photoBox = new LinearLayout(getContext());
                        photoBox.setOrientation(LinearLayout.VERTICAL);
                        photoBox.setTag("photo_box_" + qId);

                        // a) botón “Tomar foto”
                        Button btnPhoto = new Button(getContext());
                        btnPhoto.setText("Tomar foto");
                        photoBox.addView(btnPhoto);

                        // b) ImageView
                        ImageView iv = new ImageView(getContext());
                        iv.setTag("iv_" + qId);
                        iv.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        iv.setVisibility(View.GONE);
                        photoBox.addView(iv);

                        // c) etiqueta
                        TextView tvLabel = new TextView(getContext());
                        tvLabel.setTag("label_" + qId);
                        tvLabel.setText("Foto capturada");
                        tvLabel.setVisibility(View.GONE);
                        photoBox.addView(tvLabel);

                        // d) botón eliminar
                        Button btnDel = new Button(getContext());
                        btnDel.setTag("del_" + qId);
                        btnDel.setText("X");
                        btnDel.setVisibility(View.GONE);
                        photoBox.addView(btnDel);

                        // --- Aquí el cambio clave: ---
                        // Añadimos photoBox AL ITEM inflado, no al parámetro container
                        ((LinearLayout) item).addView(photoBox);

                        // Precarga y listeners (idéntico a como lo tenías)
                        Uri existing = vm.getPhotoUri(qId);
                        if (existing != null) {
                            iv.setImageURI(existing);
                            iv.setVisibility(View.VISIBLE);
                            tvLabel.setVisibility(View.VISIBLE);
                            btnDel.setVisibility(View.VISIBLE);
                        }

                        btnPhoto.setOnClickListener(v -> {
                            currentPhotoQuestionId = qId;
                            cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        });

                        btnDel.setOnClickListener(v -> {
                            vm.setPhotoUri(qId, null);
                            iv.setVisibility(View.GONE);
                            tvLabel.setVisibility(View.GONE);
                            btnDel.setVisibility(View.GONE);
                        });
                    }

                    // Al cambiar selección, lo guardamos en VM
                    rg.setOnCheckedChangeListener((group, checkedId) -> {
                        if (checkedId != -1) {
                            RadioButton sel = group.findViewById(checkedId);
                            vm.setAnswer(qId, sel.getText().toString());
                        }
                    });
                    break;

                case "MULTIPLE_OPTION":
                    // 1) Inflamos el item contenedor
                    item = inflater.inflate(
                            R.layout.item_question_multiple_option,
                            llContainer,
                            false
                    );
                    TextView tvLabel = item.findViewById(R.id.tvQuestionLabel);
                    tvLabel.setText(p.getTextoPregunta());

                    // 2) Buscamos el LinearLayout donde pondremos los CheckBox
                    LinearLayout multiContainer = item.findViewById(R.id.llMultipleOptions);

                    // 3) Por cada opción creamos un CheckBox
                    for (OpcionRespuesta opt : p.getOpciones()) {
                        CheckBox cb = new CheckBox(getContext());
                        cb.setText(opt.getDescripcion());
                        cb.setTag("cb_" + qId + "_" + opt.getDescripcion());

                        // Precarga
                        List<String> sel = vm.getMultiAnswers(qId);
                        cb.setChecked(sel.contains(opt.getDescripcion()));

                        // Listener
                        cb.setOnCheckedChangeListener((button, checked) ->
                                vm.setMultiAnswer(qId, opt.getDescripcion(), checked)
                        );

                        multiContainer.addView(cb);
                    }
                    break;

                case "PHOTO":
                    final View photoItem = item;
                    Button btnCapture = item.findViewById(R.id.btnCapturePhoto);
                    btnCapture.setOnClickListener(v -> {
                        // aquí usamos qId y photoItem de forma segura
                        cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        photoUris.put(qId, null);
                        photoItem.setTag("photoTarget_" + qId);
                    });
                    break;
            }

            if (item != null) {
                llContainer.addView(item);
            }
        }

        // Botón de envío en la última sección
        if (isLast) {
            Button btnEnviar = new Button(getContext());
            btnEnviar.setText("Enviar Ficha");
            btnEnviar.setOnClickListener(v ->
                    ((FichaQuestionsActivity) requireActivity()).submitAll()
            );
            llContainer.addView(btnEnviar);
        }

        return root;
    }

    private void setupActivityResultLaunchers() {
        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(getContext(),
                                "Permiso de cámara denegado",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        // Una vez concedido, lanzamos la cámara
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && currentPhotoQuestionId != null) {

                        Bitmap foto = (Bitmap) result.getData()
                                .getExtras().get("data");

                        // guardamos en media store y en VM
                        Uri savedUri = UriUtils.saveBitmapReturnUri(
                                getContext(), foto, currentPhotoQuestionId + ".jpg"
                        );
                        vm.setPhotoUri(currentPhotoQuestionId, savedUri);

                        // actualizar UI
                        ImageView iv = llContainer.findViewWithTag("iv_" + currentPhotoQuestionId);
                        TextView tvLabel = llContainer.findViewWithTag("label_" + currentPhotoQuestionId);
                        Button btnDel  = llContainer.findViewWithTag("del_" + currentPhotoQuestionId);

                        iv.setImageBitmap(foto);
                        iv.setVisibility(View.VISIBLE);
                        tvLabel.setVisibility(View.VISIBLE);
                        btnDel.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    public interface OnHeaderDocenteChange {
        void onHeaderDocenteChanged(String nuevoDocente);
    }

    private OnHeaderDocenteChange listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnHeaderDocenteChange) {
            listener = (OnHeaderDocenteChange) context;
        }
    }
}