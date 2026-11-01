#define ARMOR3D_METADATA_HEADER_TEXELS 6
#define ARMOR3D_CARRIER_RECORD_TEXELS 8
#define ARMOR3D_VERTICES_PER_FACE 4
#define ARMOR3D_FACES_PER_CARRIER 6
#define ARMOR3D_VERTICES_PER_CARRIER 24
#define ARMOR3D_FACE_NONE 255
#define ARMOR3D_VARIANT_SKELETON 254
#define ARMOR3D_MODEL_MARKER_ALPHA 250
#define ARMOR3D_FALLBACK_MARKER_ALPHA 252
#define ARMOR3D_NEIGHBOUR_FALLBACK_ALPHA 251
#define ARMOR3D_LAYER_TYPE_MASK 31
#define ARMOR3D_DIRECT_VERTICES_FLAG 32
#define ARMOR3D_FRAME_INTERPOLATION_FLAG 64
#define ARMOR3D_FULLBRIGHT_FLAG 128
#define ARMOR3D_LAYER_FLAG_EMISSIVE 1
#define ARMOR3D_LAYER_FLAG_TINTED 2
#define ARMOR3D_BOOLEAN_MASK 1
#define ARMOR3D_CARRIER_FLIP_XY_FLAG 1
#define ARMOR3D_CARRIER_FACE_EAST 2
#define ARMOR3D_CARRIER_FACE_DOWN 3
#define ARMOR3D_CARRIER_FACE_WEST 4
#define ARMOR3D_CARRIER_FACE_SOUTH 5
#define ARMOR3D_HIDDEN_POSITION 9999.0
#define ARMOR3D_MARKER_COLOR ivec3(11, 45, 14)
#define ARMOR3D_ANIMATION_TIME_SCALE 24000.0
#define ARMOR3D_UV_JITTER_SCALE 0.0001

armor3dActive = 0;
armor3dGeometryInvalid = 0.0;
armor3dFrameBlend = 0.0;
armor3dLayerFlags = 0;
armor3dEmissiveMaskOffsetV = 0.0;
armor3dTintMaskOffsetV = 0.0;
armor3dFullbright = 0;
armor3dNextTexCoord = vec2(0.0);
#ifdef ARMOR3D_TEXTURE_ANIMATION
armor3dAnimationMasks = vec4(-2.0);
#endif
bool armor3dHidden = false;

ivec4 armor3dMarker = ivec4(
    texelFetch(Sampler0, ivec2(0), 0) * ARMOR3D_TEXEL_CHANNEL_SCALE
    + ARMOR3D_TEXEL_ROUNDING_BIAS
);

if (armor3dMarker.rgb == ARMOR3D_MARKER_COLOR
    && armor3dMarker.a == ARMOR3D_MODEL_MARKER_ALPHA) {
    armor3dActive = 1;
#ifdef ARMOR3D_SUBGROUP_SUPPORTED
    ivec2 metadataOrigin = ivec2(0);
    ivec4 modelHeader = armor3dReadMetadata(metadataOrigin, 1);
    int equipmentLayerType = modelHeader.r & ARMOR3D_LAYER_TYPE_MASK;
    bool usesHumanoidCarrier = equipmentLayerType == ARMOR3D_LAYER_HUMANOID
        || equipmentLayerType == ARMOR3D_LAYER_HUMANOID_LEGGINGS
        || equipmentLayerType == ARMOR3D_LAYER_HUMANOID_BABY;
    vec2 carrierUvDimensions = vec2(max(modelHeader.b, 1), max(modelHeader.a, 1));
    int carrierCount = max(modelHeader.g, 1);
    int verticesPerDraw = carrierCount * ARMOR3D_VERTICES_PER_CARRIER;
    int drawVertexIndex = (ARMOR3D_VERTEX_ID - gl_BaseVertexARB) % verticesPerDraw;
    int carrierFaceIndex = drawVertexIndex / ARMOR3D_VERTICES_PER_FACE;
    int carrierIndex = carrierFaceIndex / ARMOR3D_FACES_PER_CARRIER;
    int carrierFace = carrierFaceIndex % ARMOR3D_FACES_PER_CARRIER;
    bool neighbourLookup = armor3dUsesNeighbourLookup(equipmentLayerType);
    Armor3dFace boundaryCarrier = Armor3dFace(
        uint[4](0u, 0u, 0u, 0u),
        vec3[4](vec3(0.0), vec3(0.0), vec3(0.0), vec3(0.0)),
        vec2[4](vec2(0.0), vec2(0.0), vec2(0.0), vec2(0.0)),
        false,
        false
    );
    int boundaryGeometryFace = carrierFace;
    if (neighbourLookup) {
        boundaryCarrier = armor3dGatherFace();
        boundaryCarrier = armor3dExpandReinFace(boundaryCarrier, equipmentLayerType, carrierUvDimensions);
        carrierIndex = armor3dNeighbourCarrier(equipmentLayerType, carrierCount, carrierUvDimensions,
            boundaryCarrier, boundaryGeometryFace);
    }

    if (usesHumanoidCarrier && carrierCount >= 3) {
        int entityVariant = int(
            Color.r * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
        );
        if (entityVariant == ARMOR3D_VARIANT_SKELETON) {
            if (carrierIndex == 1) carrierIndex = 2;
            else if (carrierIndex == 2) carrierIndex = 1;
        }
    }

    int carrierRecordOffset = ARMOR3D_METADATA_HEADER_TEXELS
        + carrierIndex * ARMOR3D_CARRIER_RECORD_TEXELS;
    int faceVertexIndex = drawVertexIndex % ARMOR3D_VERTICES_PER_FACE;
    int packedFaceSlot = neighbourLookup ? 0 : carrierFace == ARMOR3D_CARRIER_FACE_DOWN ? 0
        : carrierFace == ARMOR3D_CARRIER_FACE_SOUTH ? 1
        : carrierFace == ARMOR3D_CARRIER_FACE_EAST ? 2
        : carrierFace == ARMOR3D_CARRIER_FACE_WEST ? 3
        : carrierFace == 0 ? 4
        : carrierFace == 1 ? 5
        : -1;
    bool hideVertex = packedFaceSlot < 0 || carrierIndex < 0 || carrierIndex >= carrierCount;
    int modelFaceIndex = ARMOR3D_FACE_NONE;
    ivec4 verticalData = ivec4(255, 255, 0, 0);
    if (!hideVertex) {
        if (packedFaceSlot >= 4 || neighbourLookup) {
            verticalData = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 7);
        }
        if (packedFaceSlot >= 4) {
            modelFaceIndex = packedFaceSlot == 4 ? verticalData.r : verticalData.g;
        } else {
            ivec4 packedFaces = armor3dReadMetadata(metadataOrigin, carrierRecordOffset);
            modelFaceIndex = packedFaceSlot == 0 ? packedFaces.r
                : packedFaceSlot == 1 ? packedFaces.g
                : packedFaceSlot == 2 ? packedFaces.b
                : packedFaces.a;
        }
        hideVertex = modelFaceIndex == ARMOR3D_FACE_NONE;
    }
    if (hideVertex) {
        transformedPosition = vec3(ARMOR3D_HIDDEN_POSITION);
        armor3dHidden = true;
    } else {
        Armor3dFace carrier;
        if (neighbourLookup) carrier = boundaryCarrier;
        else carrier = armor3dGatherFace();
        int geometryFace = neighbourLookup ? boundaryGeometryFace : carrierFace;
        ivec4 carrierControl = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 1);
        bool flipCarrierXY = (carrierControl.b & ARMOR3D_CARRIER_FLIP_XY_FLAG) != 0;
        if (carrier.valid) {
            carrier = armor3dCarrierFace(carrier, equipmentLayerType, carrierControl.g, flipCarrierXY,
                carrierUvDimensions, geometryFace);
        }
        if (!carrier.valid) {
            armor3dGeometryInvalid = 1.0;
        } else {
            vec3 carrierCorner0 = carrier.corners[0];
            vec3 carrierCorner1 = carrier.corners[1];
            vec3 carrierCorner3 = carrier.corners[3];
            ivec4 carrierSpanData = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 6);
            float carrierFaceSpan = ((
                geometryFace == ARMOR3D_CARRIER_FACE_EAST
                || geometryFace == ARMOR3D_CARRIER_FACE_WEST
            )
                ? float(carrierSpanData.b * ARMOR3D_BYTE_BASE + carrierSpanData.a)
                : float(carrierSpanData.r * ARMOR3D_BYTE_BASE + carrierSpanData.g))
                / float(ARMOR3D_BYTE_BASE);
            vec3 carrierWidthEdge = carrierCorner0 - carrierCorner1;
            float carrierWidth = length(carrierWidthEdge);
            float modelScale = carrierWidth * 16.0 / carrierFaceSpan;
            ivec4 positionLayout = armor3dReadMetadata(metadataOrigin, 2);
            ivec4 geometryLayout = armor3dReadMetadata(metadataOrigin, 3);
            ivec4 uvLayoutHeader = armor3dReadMetadata(metadataOrigin, 4);
            ivec4 animationData = armor3dReadMetadata(metadataOrigin, 5);
            ivec4 maskOffsets = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 2);
            ivec4 calibration0 = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 3);
            ivec4 calibration1 = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 4);
            ivec4 calibration2 = armor3dReadMetadata(metadataOrigin, carrierRecordOffset + 5);

            ivec2 atlasDimensions = textureSize(Sampler0, 0);
            vec2 inverseAtlasDimensions = 1.0 / vec2(atlasDimensions);
            ivec2 modelDimensions = ivec2(
                atlasDimensions.x,
                geometryLayout.a * ARMOR3D_BYTE_BASE + uvLayoutHeader.a
            );
            int frameCount = max(animationData.a, 1);
            armor3dFullbright = (modelHeader.r & ARMOR3D_FULLBRIGHT_FLAG) != 0 ? 1 : 0;
            int emissiveMaskPixelOffset = maskOffsets.r * ARMOR3D_BYTE_BASE + maskOffsets.g;
            int tintMaskPixelOffset = maskOffsets.b * ARMOR3D_BYTE_BASE + maskOffsets.a;
            if (emissiveMaskPixelOffset == ARMOR3D_UNSIGNED_SHORT_MAX) {
                armor3dLayerFlags |= ARMOR3D_LAYER_FLAG_EMISSIVE;
            } else if (emissiveMaskPixelOffset > 0) {
                armor3dEmissiveMaskOffsetV = float(emissiveMaskPixelOffset)
                    * inverseAtlasDimensions.y;
            }
            if (tintMaskPixelOffset == ARMOR3D_UNSIGNED_SHORT_MAX) {
                armor3dLayerFlags |= ARMOR3D_LAYER_FLAG_TINTED;
            } else if (tintMaskPixelOffset > 0) {
                armor3dTintMaskOffsetV = float(tintMaskPixelOffset) * inverseAtlasDimensions.y;
            }
            int faceFullbright = (carrierControl.r >> packedFaceSlot) & ARMOR3D_BOOLEAN_MASK;
            ivec3 packedCarrierOffset = ivec3(
                calibration0.r * ARMOR3D_BYTE_BASE + calibration0.g,
                calibration0.b * ARMOR3D_BYTE_BASE + calibration0.a,
                calibration1.r * ARMOR3D_BYTE_BASE + calibration1.g
            );
            packedCarrierOffset = bitfieldExtract(packedCarrierOffset, 0, 16);
            vec3 carrierOffset = vec3(packedCarrierOffset) / ARMOR3D_POSITION_SCALE;
            ivec3 packedAnchorDepth = ivec3(
                calibration1.b * ARMOR3D_BYTE_BASE + calibration1.a,
                calibration2.r * ARMOR3D_BYTE_BASE + calibration2.g,
                calibration2.b * ARMOR3D_BYTE_BASE + calibration2.a
            );
            packedAnchorDepth = bitfieldExtract(packedAnchorDepth, 0, 16);
            vec3 anchorDepth = vec3(packedAnchorDepth) / ARMOR3D_POSITION_SCALE;
            if (faceFullbright > 0) armor3dFullbright = 1;

            int modelVertexIndex = modelFaceIndex * ARMOR3D_VERTICES_PER_FACE + faceVertexIndex;
            int textureDataStartRow = positionLayout.a;
            int positionDataStartRow = armor3dDecodeRow(positionLayout);
            int uvDataStartRow = armor3dDecodeRow(geometryLayout);
            int vertexDataStartRow = armor3dDecodeRow(uvLayoutHeader);
            ivec2 vertexData;
            if ((modelHeader.r & ARMOR3D_DIRECT_VERTICES_FLAG) != 0) {
                vertexData = ivec2(modelVertexIndex * 3, modelVertexIndex * 3 + 2);
            } else {
                vertexData = armor3dReadVertex(
                    metadataOrigin,
                    modelDimensions.x,
                    vertexDataStartRow,
                    modelVertexIndex
                );
                vertexData.x *= 2;
            }
            vec3 modelOffset = armor3dReadPosition(
                metadataOrigin,
                modelDimensions.x,
                positionDataStartRow,
                vertexData.x
            );
            vec2 modelUv = armor3dReadUv(
                metadataOrigin,
                modelDimensions.x,
                uvDataStartRow,
                vertexData.y
            );

            vec2 uvJitter = inverseAtlasDimensions
                * vec2(
                    float(faceVertexIndex),
                    float((faceVertexIndex + 1) % ARMOR3D_VERTICES_PER_FACE)
                )
                * ARMOR3D_UV_JITTER_SCALE;
            texCoord = (
                vec2(0, textureDataStartRow) + modelUv * vec2(modelDimensions)
            ) * inverseAtlasDimensions + uvJitter;

            if (frameCount > 1) {
                float frameDuration = max(float(
                    animationData.r * ARMOR3D_UINT16_BASE
                    + animationData.g * ARMOR3D_BYTE_BASE
                    + animationData.b
                ), 1.0);
                float animationTime = GameTime * ARMOR3D_ANIMATION_TIME_SCALE;
                int currentFrame = int(animationTime / frameDuration) % frameCount;
                int nextFrame = (currentFrame + 1) % frameCount;
                texCoord = (
                    vec2(0, textureDataStartRow + currentFrame * modelDimensions.y)
                    + modelUv * vec2(modelDimensions)
                ) * inverseAtlasDimensions + uvJitter;
                armor3dNextTexCoord = (
                    vec2(0, textureDataStartRow + nextFrame * modelDimensions.y)
                    + modelUv * vec2(modelDimensions)
                ) * inverseAtlasDimensions + uvJitter;
                armor3dFrameBlend = (modelHeader.r & ARMOR3D_FRAME_INTERPOLATION_FLAG) != 0
                    ? fract(animationTime / frameDuration)
                    : 0.0;
            }
#ifdef ARMOR3D_TEXTURE_ANIMATION
            if (animationData.a == 0) {
                armor3dAnimate(animationData, modelFaceIndex, modelUv, modelDimensions, uvJitter);
            }
#endif
            modelOffset -= carrierOffset;
            if (flipCarrierXY) modelOffset.xy = -modelOffset.xy;

            vec3 modelCorner0 = carrier.corners[0];
            vec3 modelCorner1 = carrier.corners[1];
            vec3 modelCorner2 = carrier.corners[2];
            vec3 modelCorner3 = carrier.corners[3];
            vec3 basisX = carrierWidthEdge / carrierWidth;
            vec3 carrierHeightEdge = modelCorner0 - modelCorner3;
            vec3 basisY = normalize(carrierHeightEdge - dot(carrierHeightEdge, basisX) * basisX);
            vec3 basisNormal = cross(basisX, basisY);

            if (ProjMat[2][3] == 0.0) basisNormal = -basisNormal;

            bool verticalGeometry = geometryFace == 0 || geometryFace == 1;
            mat3 faceBasis = verticalGeometry
                ? armor3dVerticalBasis(basisX, basisY, basisNormal, geometryFace == 1, flipCarrierXY)
                : geometryFace == ARMOR3D_CARRIER_FACE_SOUTH
                ? mat3( basisX,  basisY,  basisNormal)
                : geometryFace == ARMOR3D_CARRIER_FACE_EAST
                    ? mat3( basisNormal,  basisY, -basisX)
                : geometryFace == ARMOR3D_CARRIER_FACE_WEST
                    ? mat3(-basisNormal,  basisY,  basisX)
                : mat3(-basisX, basisY, -basisNormal);
            modelOffset = faceBasis * modelOffset;

            vec3 faceAnchor;
            if (verticalGeometry) {
                float carrierHeight = float(verticalData.b * ARMOR3D_BYTE_BASE + verticalData.a)
                    / (16.0 * float(ARMOR3D_BYTE_BASE)) * modelScale;
                faceAnchor = armor3dVerticalAnchor(modelCorner1, modelCorner2, basisNormal,
                    carrierHeight, geometryFace == 1, flipCarrierXY);
            } else if (geometryFace == ARMOR3D_CARRIER_FACE_DOWN) {
                faceAnchor = modelCorner2;
            } else if (geometryFace == ARMOR3D_CARRIER_FACE_EAST) {
                faceAnchor = modelCorner2 + carrierWidthEdge;
            } else if (geometryFace == ARMOR3D_CARRIER_FACE_SOUTH) {
                faceAnchor = modelCorner2 + carrierWidthEdge
                    - anchorDepth.y * carrierWidth * basisNormal;
            } else {
                faceAnchor = modelCorner2 - anchorDepth.z * carrierWidth * basisNormal;
            }
            transformedPosition = faceAnchor + modelOffset * modelScale;

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
            if (armor3dFullbright == 0) {
                vec3 modelNormal = armor3dReadNormal(metadataOrigin, modelFaceIndex);
                if (flipCarrierXY) modelNormal.xy = -modelNormal.xy;
                armor3dFaceNormal = faceBasis * modelNormal;
                if (ProjMat[2][3] == 0.0) armor3dFaceNormal = -armor3dFaceNormal;
            }
#endif
        }
    }
#else
    transformedPosition = vec3(ARMOR3D_HIDDEN_POSITION);
    armor3dHidden = true;
#endif
}
#ifdef ARMOR3D_SUBGROUP_SUPPORTED
else if (
    armor3dMarker.rgb == ARMOR3D_MARKER_COLOR
    && (armor3dMarker.a == ARMOR3D_FALLBACK_MARKER_ALPHA
        || armor3dMarker.a == ARMOR3D_NEIGHBOUR_FALLBACK_ALPHA)
    && !(armor3dMarker.a == ARMOR3D_NEIGHBOUR_FALLBACK_ALPHA && gl_SubgroupSize < 16u)
) {
    Armor3dFace carrier = armor3dGatherFace();
    if (carrier.valid) {
        transformedPosition = vec3(ARMOR3D_HIDDEN_POSITION);
        armor3dHidden = true;
    }
}
#endif
