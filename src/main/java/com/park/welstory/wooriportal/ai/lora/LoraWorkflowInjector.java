package com.park.welstory.wooriportal.ai.lora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class LoraWorkflowInjector {

    private static final String LORA_NODE_ID = "lora_dyn_01";

    private final ObjectMapper mapper;

    public LoraWorkflowInjector(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  t2i 워크플로우용
    //    모델노드: "75:70"   클립노드: "75:71"
    //    CFGGuider: "75:63"  CLIPTextEncode(pos): "75:74"  (neg): "75:67"
    // ──────────────────────────────────────────────────────────────────────────

    public void injectT2i(ObjectNode wf, Optional<LoraConfig> loraOpt) {
        if (loraOpt.isEmpty()) return;
        LoraConfig lora = loraOpt.get();
        injectLora(wf,
                lora,
                "75:70",   // unet loader
                "75:71",   // clip loader
                "75:63",   // CFGGuider  → model input
                "75:74",   // positive CLIP encode → clip input
                "75:67"    // negative CLIP encode → clip input
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  i2i 워크플로우용
    //    모델노드: "92:107"   클립노드: "92:108"
    //    CFGGuider: "92:103"  CLIPTextEncode(pos): "92:109"
    //    (i2i negative는 ConditioningZeroOut이므로 clip 연결 불필요)
    // ──────────────────────────────────────────────────────────────────────────

    public void injectI2i(ObjectNode wf, Optional<LoraConfig> loraOpt) {
        if (loraOpt.isEmpty()) return;
        LoraConfig lora = loraOpt.get();
        injectLora(wf,
                lora,
                "92:107",  // unet loader
                "92:108",  // clip loader
                "92:103",  // CFGGuider → model input
                "92:109",  // positive CLIP encode → clip input
                null       // i2i에는 negative clip 직접 연결 없음
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  painter 워크플로우용 (painter.json)
    //    Power Lora Loader (rgthree) 노드(144)가 이미 체인에 포함되어 있음
    //    UNETLoader(105) → PowerLoraLoader(144) → KSampler(117)
    //    → node144 inputs에 lora_1 오브젝트만 주입하면 됨
    //    clip도 node144 출력(1)을 PainterFluxImageEdit(116)에 연결해야 하지만
    //    painter.json에서 node116.clip은 이미 node106(CLIPLoader) 직결이므로
    //    clip 강도 적용을 위해 node116.clip을 node144 출력(1)으로 교체
    // ──────────────────────────────────────────────────────────────────────────

    public void injectPainter(ObjectNode wf, Optional<LoraConfig> loraOpt) {
        if (loraOpt.isEmpty()) return;
        LoraConfig lora = loraOpt.get();

        // 1. Power Lora Loader(144) inputs에 lora_1 슬롯 주입
        ObjectNode loraSlot = mapper.createObjectNode();
        loraSlot.put("on",           true);
        loraSlot.put("lora",         lora.filename);
        loraSlot.put("strength",     lora.strength);
        loraSlot.put("strengthTwo",  lora.strength); // CLIP strength
        ((ObjectNode) wf.path("144").path("inputs")).set("lora_1", loraSlot);

        // 2. PainterFluxImageEdit(116)의 clip 입력을 PowerLoraLoader(144) 출력(1)으로 교체
        //    → LoRA의 clip 가중치가 실제로 반영되도록
        if (wf.has("116")) {
            ((ObjectNode) wf.path("116").path("inputs"))
                    .set("clip", ref("144", 1));
        }
    }

    public void injectPainterV2(ObjectNode wf, Optional<LoraConfig> loraOpt) {
        if (loraOpt.isEmpty()) return;
        LoraConfig lora = loraOpt.get();

        // 1. LoraLoader 노드 생성 (200→LoraLoader→다운스트림)
        ObjectNode loraNode   = mapper.createObjectNode();
        ObjectNode loraInputs = mapper.createObjectNode();
        loraInputs.put("lora_name",      lora.filename);
        loraInputs.put("strength_model", lora.strength);
        loraInputs.put("strength_clip",  lora.strength);
        loraInputs.set("model", ref("200", 0));  // UNETLoader
        loraInputs.set("clip",  ref("201", 0));  // CLIPLoaderGGUF
        loraNode.set("inputs", loraInputs);
        loraNode.put("class_type", "LoraLoader");
        ObjectNode meta = mapper.createObjectNode();
        meta.put("title", "LoRA: " + lora.name());
        loraNode.set("_meta", meta);
        wf.set(LORA_NODE_ID, loraNode);  // "lora_dyn_01"

        // 2. KSampler(208) model → LoraLoader 출력(0)
        if (wf.has("208")) {
            ((ObjectNode) wf.path("208").path("inputs"))
                    .set("model", ref(LORA_NODE_ID, 0));
        }
        // 3. LanPaint KSampler(223) model → LoraLoader 출력(0)
        if (wf.has("223")) {
            ((ObjectNode) wf.path("223").path("inputs"))
                    .set("model", ref(LORA_NODE_ID, 0));
        }
        // 4. PainterFluxImageEdit(207) clip → LoraLoader 출력(1)
        if (wf.has("207")) {
            ((ObjectNode) wf.path("207").path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }
        // 5. CLIPTextEncode 2차 positive(216) clip → LoraLoader 출력(1)
        if (wf.has("216")) {
            ((ObjectNode) wf.path("216").path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }
        // 6. T2I positive(301) clip → LoraLoader 출력(1)
        if (wf.has("301")) {
            ((ObjectNode) wf.path("301").path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }
        // 7. T2I negative(302) clip → LoraLoader 출력(1)
        if (wf.has("302")) {
            ((ObjectNode) wf.path("302").path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  공통 삽입 로직
    // ──────────────────────────────────────────────────────────────────────────

    private void injectLora(ObjectNode wf,
                            LoraConfig lora,
                            String unetNodeId,
                            String clipNodeId,
                            String cfgNodeId,
                            String posClipNodeId,
                            String negClipNodeId) {
        // 1. LoraLoader 노드 생성
        ObjectNode loraNode   = mapper.createObjectNode();
        ObjectNode loraInputs = mapper.createObjectNode();

        loraInputs.put("lora_name",     lora.filename);
        loraInputs.put("strength_model", lora.strength);
        loraInputs.put("strength_clip",  lora.strength);
        loraInputs.set("model", ref(unetNodeId, 0));
        loraInputs.set("clip",  ref(clipNodeId, 0));
        loraNode.set("inputs", loraInputs);
        loraNode.put("class_type", "LoraLoader");
        ObjectNode loraMeta = mapper.createObjectNode();
        loraMeta.put("title", "LoRA: " + lora.name());
        loraNode.set("_meta", loraMeta);

        wf.set(LORA_NODE_ID, loraNode);

        // 2. CFGGuider의 model 입력을 LoraLoader 출력(0)으로 교체
        if (cfgNodeId != null && wf.has(cfgNodeId)) {
            ((ObjectNode) wf.path(cfgNodeId).path("inputs"))
                    .set("model", ref(LORA_NODE_ID, 0));
        }

        // 3. Positive CLIPTextEncode의 clip 입력을 LoraLoader 출력(1)으로 교체
        if (posClipNodeId != null && wf.has(posClipNodeId)) {
            ((ObjectNode) wf.path(posClipNodeId).path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }

        // 4. Negative CLIPTextEncode의 clip 입력 교체 (t2i만 해당)
        if (negClipNodeId != null && wf.has(negClipNodeId)) {
            ((ObjectNode) wf.path(negClipNodeId).path("inputs"))
                    .set("clip", ref(LORA_NODE_ID, 1));
        }
    }

    /** ComfyUI 노드 참조 배열 생성: ["nodeId", outputIndex] */
    private ArrayNode ref(String nodeId, int outputIndex) {
        ArrayNode arr = mapper.createArrayNode();
        arr.add(nodeId);
        arr.add(outputIndex);
        return arr;
    }
}