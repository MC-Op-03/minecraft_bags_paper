#define ARMOR3D_TEXEL_CHANNEL_SCALE 255.0
#define ARMOR3D_TEXEL_ROUNDING_BIAS 0.5
#define ARMOR3D_NIBBLE_BASE 16
#define ARMOR3D_BYTE_BASE 256
#define ARMOR3D_UINT16_BASE 65536
#define ARMOR3D_POSITION_PACK_FACTOR 4096
#define ARMOR3D_POSITION_SCALE 4096.0
#define ARMOR3D_POSITION_BIAS 128.0
#define ARMOR3D_UNSIGNED_SHORT_MAX 65535
#define ARMOR3D_UV_NORMALIZATION 65535.0
#define ARMOR3D_LAYER_WOLF_BODY 1
#define ARMOR3D_LAYER_HORSE_BODY 2
#define ARMOR3D_LAYER_LLAMA_BODY 3
#define ARMOR3D_LAYER_HUMANOID 4
#define ARMOR3D_LAYER_HUMANOID_LEGGINGS 5
#define ARMOR3D_LAYER_WINGS 6
#define ARMOR3D_LAYER_PIG_SADDLE 7
#define ARMOR3D_LAYER_STRIDER_SADDLE 8
#define ARMOR3D_LAYER_CAMEL_SADDLE 9
#define ARMOR3D_LAYER_HORSE_SADDLE 10
#define ARMOR3D_LAYER_DONKEY_SADDLE 11
#define ARMOR3D_LAYER_MULE_SADDLE 12
#define ARMOR3D_LAYER_SKELETON_HORSE_SADDLE 13
#define ARMOR3D_LAYER_ZOMBIE_HORSE_SADDLE 14
#define ARMOR3D_LAYER_HAPPY_GHAST_BODY 15
#define ARMOR3D_LAYER_NAUTILUS_BODY 16
#define ARMOR3D_LAYER_NAUTILUS_SADDLE 17
#define ARMOR3D_LAYER_CAMEL_HUSK_SADDLE 18
#define ARMOR3D_LAYER_HUMANOID_BABY 19

mat3 armor3dVerticalBasis(vec3 basisX, vec3 basisY, vec3 basisNormal, bool upperFace, bool flipXY) {
    vec3 verticalY = flipXY ? -basisNormal : basisNormal;
    vec3 verticalZ = flipXY ? -basisY : basisY;
    return upperFace ? mat3(-basisX, -verticalY, -verticalZ) : mat3(-basisX, verticalY, verticalZ);
}

vec3 armor3dVerticalAnchor(vec3 corner1, vec3 corner2, vec3 basisNormal, float height, bool upperFace, bool flipXY) {
    return upperFace == flipXY ? corner2 - height * basisNormal : corner1;
}

#if defined(GL_KHR_shader_subgroup_basic) && defined(GL_KHR_shader_subgroup_ballot) && defined(GL_KHR_shader_subgroup_shuffle) && defined(GL_ARB_shader_draw_parameters) && (!defined(VULKAN) || __VERSION__ >= 440)
#define ARMOR3D_SUBGROUP_SUPPORTED
#ifdef VULKAN
#define ARMOR3D_VERTEX_ID gl_VertexIndex
#define ARMOR3D_INSTANCE_ID gl_InstanceIndex
#else
#define ARMOR3D_VERTEX_ID gl_VertexID
#define ARMOR3D_INSTANCE_ID gl_InstanceID
#endif

struct Armor3dFace {
    uint lanes[4];
    vec3 corners[4];
    vec2 uvs[4];
    bool complete;
    bool valid;
};

vec4 armor3dCarrierUvBox(int layerType, int part, vec2 uvMin) {
    if (layerType == ARMOR3D_LAYER_HUMANOID || layerType == ARMOR3D_LAYER_HUMANOID_LEGGINGS) {
        return vec4(part == 0 ? 16.0 : part == 2 || part == 3 ? 40.0 : 0.0,
            part == 1 ? 0.0 : 16.0, part == 0 || part == 1 ? 8.0 : 4.0, part == 1 ? 8.0 : 4.0);
    }
    if (layerType == ARMOR3D_LAYER_WINGS) return vec4(22.0, 0.0, 10.0, 2.0);
    if (layerType == ARMOR3D_LAYER_WOLF_BODY) {
        if (part == 0) return vec4(18.0, 14.0, 6.0, 6.0);
        if (part == 1) return vec4(0.0, 0.0, 6.0, 4.0);
        if (part == 2) return vec4(21.0, 0.0, 8.0, 7.0);
        return vec4(part == 3 ? 9.0 : 0.0, 18.0, 2.0, 2.0);
    }
    if (layerType == ARMOR3D_LAYER_HORSE_BODY
        || (layerType >= ARMOR3D_LAYER_HORSE_SADDLE && layerType <= ARMOR3D_LAYER_ZOMBIE_HORSE_SADDLE)) {
        if (part == 7 || part == 8) return vec4(32.0, 2.0, 0.0, 16.0);
        if (part == 0) return vec4(0.0, 32.0, 10.0, 22.0);
        if (part == 1) return vec4(0.0, 35.0, 4.0, 7.0);
        if (part == 2) return vec4(42.0, 36.0, 3.0, 4.0);
        return vec4(48.0, 21.0, 4.0, 4.0);
    }
    if (layerType == ARMOR3D_LAYER_LLAMA_BODY) {
        if (part == 0) return vec4(29.0, 0.0, 12.0, 10.0);
        if (part == 1) return vec4(0.0, 0.0, 4.0, 9.0);
        if (part == 2) return vec4(29.0, 29.0, 4.0, 4.0);
        if (part == 3) return vec4(29.0, 29.0, 4.0, 4.0);
        if (part == 4) return vec4(29.0, 29.0, 4.0, 4.0);
        if (part == 5) return vec4(29.0, 29.0, 4.0, 4.0);
        if (part == 6) return vec4(45.0, 28.0, 8.0, 3.0);
        if (part == 7) return vec4(45.0, 41.0, 8.0, 3.0);
    }
    if (layerType == ARMOR3D_LAYER_PIG_SADDLE) {
        if (part == 0) return vec4(28.0, 8.0, 10.0, 8.0);
        if (part == 1) return vec4(0.0, 0.0, 8.0, 8.0);
        if (part == 2) return vec4(0.0, 16.0, 4.0, 4.0);
        if (part == 3) return vec4(0.0, 16.0, 4.0, 4.0);
        if (part == 4) return vec4(0.0, 16.0, 4.0, 4.0);
        if (part == 5) return vec4(0.0, 16.0, 4.0, 4.0);
    }
    if (layerType == ARMOR3D_LAYER_STRIDER_SADDLE) {
        if (part == 0) return vec4(0.0, 0.0, 16.0, 16.0);
        if (part == 1) return vec4(0.0, 32.0, 4.0, 4.0);
        if (part == 2) return vec4(0.0, 55.0, 4.0, 4.0);
    }
    if (layerType == ARMOR3D_LAYER_CAMEL_SADDLE || layerType == ARMOR3D_LAYER_CAMEL_HUSK_SADDLE) {
        if (part == 0) return vec4(0.0, 25.0, 15.0, 27.0);
        if (part == 1) return vec4(60.0, 24.0, 7.0, 19.0);
        if (part == 2) return vec4(122.0, 0.0, 3.0, 0.0);
        if (part == 3) return vec4(0.0, 26.0, 5.0, 5.0);
        if (part == 4) return vec4(0.0, 0.0, 5.0, 5.0);
        if (part == 5) return vec4(94.0, 16.0, 5.0, 5.0);
        if (part == 6) return vec4(58.0, 16.0, 5.0, 5.0);
        if (part == 7) return vec4(67.0, 0.0, 3.0, 2.0);
        if (part == 8) return vec4(45.0, 0.0, 3.0, 2.0);
        if (part == 9) return vec4(84.0, 57.0, 7.0, 0.0);
    }
    if (layerType == ARMOR3D_LAYER_HAPPY_GHAST_BODY) {
        return part == 0 ? vec4(0.0, 0.0, 16.0, 16.0) : vec4(0.0, 32.0, 16.0, 5.0);
    }
    if (layerType == ARMOR3D_LAYER_NAUTILUS_BODY || layerType == ARMOR3D_LAYER_NAUTILUS_SADDLE) {
        if (part == 0) return vec4(0.0, 54.0, 10.0, 14.0);
        if (part == 1) return vec4(54.0, 62.0, 10.0, 4.0);
        if (part == 2) return vec4(54.0, 54.0, 10.0, 4.0);
        if (part == 3) return vec4(54.0, 70.0, 6.0, 4.0);
        return vec4(0.0, 0.0, 14.0, 16.0);
    }
    if (layerType == ARMOR3D_LAYER_HUMANOID_BABY) {
        if (part == 0) return uvMin.y >= 32.0 ? vec4(0.0, 36.0, 5.9, 2.9) : vec4(0.0, 17.0, 6.0, 3.0);
        if (part == 1) return vec4(0.0, 0.0, 9.0, 8.0);
        if (part == 2) return vec4(30.0, 25.0, 2.0, 3.0);
        if (part == 3) return vec4(30.0, 17.0, 2.0, 3.0);
        if (part == 4) return vec4(18.0, 17.0, 3.0, 3.0);
        if (part == 5) return vec4(18.0, 24.0, 3.0, 3.0);
        return vec4(0.0, part == 6 ? 25.0 : 29.0, 3.0, 3.0);
    }
    return vec4(0.0);
}

int armor3dBoxFaceIndex(vec2 uvMin, vec4 uvBox) {
    float width = uvBox.z;
    float depth = uvBox.w;
    float u = uvMin.x - uvBox.x;
    if (uvMin.y - uvBox.y < depth * 0.5) {
        return u < depth + width * 0.5 ? 0 : 1;
    }
    return u < depth * 0.5 ? 2
        : u < depth + width * 0.5 ? 3
        : u < depth * 1.5 + width ? 4 : 5;
}

int armor3dUvCorner(vec2 uv, vec2 middle, bool upper) {
    bool right = uv.x > middle.x;
    bool top = upper ? uv.y > middle.y : uv.y < middle.y;
    return right ? (top ? 0 : 3) : (top ? 1 : 2);
}

Armor3dFace armor3dCarrierFace(Armor3dFace inputFace, int layerType, int part, bool flipXY,
    vec2 uvDimensions, inout int geometryFace) {
    vec2 uvMin = min(min(inputFace.uvs[0], inputFace.uvs[1]), min(inputFace.uvs[2], inputFace.uvs[3]));
    if (layerType == ARMOR3D_LAYER_HUMANOID && part == 1 && uvMin.x * uvDimensions.x >= 32.0) {
        inputFace.valid = false;
        return inputFace;
    }
    vec2 uvMax = max(max(inputFace.uvs[0], inputFace.uvs[1]), max(inputFace.uvs[2], inputFace.uvs[3]));
    vec4 uvBox = armor3dCarrierUvBox(layerType, part, uvMin * uvDimensions);
    if (uvBox.z <= 0.0 || uvBox.w <= 0.0) return inputFace;
    geometryFace = armor3dBoxFaceIndex(uvMin * uvDimensions, uvBox);
    vec2 middle = (uvMin + uvMax) * 0.5;
    Armor3dFace face = inputFace;
    int found = 0;
    for (int k = 0; k < 4; ++k) {
        int corner = armor3dUvCorner(inputFace.uvs[k], middle, geometryFace == 1);
        if (flipXY) corner = 3 - corner;
        face.corners[corner] = inputFace.corners[k];
        face.uvs[corner] = inputFace.uvs[k];
        found |= 1 << corner;
    }
    face.valid = inputFace.valid && found == 15;
    return face;
}

Armor3dFace armor3dGatherFace() {
    Armor3dFace face;
    for (int corner = 0; corner < 4; ++corner) {
        face.lanes[corner] = gl_SubgroupInvocationID;
    }
    int faceBase = ARMOR3D_VERTEX_ID - ((ARMOR3D_VERTEX_ID - gl_BaseVertexARB) & 3);
    int found = 0;
    uvec4 activeLanes = subgroupBallot(true);
    int firstLane = int(gl_SubgroupInvocationID) - ((ARMOR3D_VERTEX_ID - gl_BaseVertexARB) & 3);
    for (int corner = 0; corner < 4; ++corner) {
        int candidate = firstLane + corner;
        uint lane = candidate >= 0 && candidate < int(gl_SubgroupSize)
            ? uint(candidate) : gl_SubgroupInvocationID;
        if (!subgroupBallotBitExtract(activeLanes, lane)) lane = gl_SubgroupInvocationID;
        ivec3 identity = subgroupShuffle(ivec3(ARMOR3D_VERTEX_ID, ARMOR3D_INSTANCE_ID, gl_DrawIDARB), lane);
        if (identity.x == faceBase + corner && identity.y == ARMOR3D_INSTANCE_ID && identity.z == gl_DrawIDARB) {
            face.lanes[corner] = lane;
            found |= 1 << corner;
        }
    }
    if (subgroupBallotBitCount(subgroupBallot(found != 15)) > 0u) {
        for (uint lane = 0u; lane < gl_SubgroupSize; ++lane) {
            if (subgroupBallotBitExtract(activeLanes, lane)) {
                ivec3 identity = subgroupShuffle(ivec3(ARMOR3D_VERTEX_ID, ARMOR3D_INSTANCE_ID, gl_DrawIDARB), lane);
                int corner = identity.x - faceBase;
                if (identity.y == ARMOR3D_INSTANCE_ID && identity.z == gl_DrawIDARB && corner >= 0 && corner < 4) {
                    face.lanes[corner] = lane;
                    found |= 1 << corner;
                }
            }
        }
    }
    for (int corner = 0; corner < 4; ++corner) {
        face.corners[corner] = subgroupShuffle(Position, face.lanes[corner]);
        face.uvs[corner] = subgroupShuffle(UV0, face.lanes[corner]);
    }
    vec3 width = face.corners[0] - face.corners[1];
    vec3 height = face.corners[0] - face.corners[3];
    vec3 area = cross(width, height);
    face.complete = found == 15;
    face.valid = face.complete && dot(width, width) > 1e-16 && dot(area, area) > 1e-20;
    return face;
}

Armor3dFace armor3dExpandReinFace(Armor3dFace face, int layerType, vec2 uvDimensions) {
    if (!face.complete || face.valid || layerType < ARMOR3D_LAYER_HORSE_SADDLE
        || layerType > ARMOR3D_LAYER_ZOMBIE_HORSE_SADDLE) return face;
    int ordinal = ((ARMOR3D_VERTEX_ID - gl_BaseVertexARB) % 24) / 4;
    if (ordinal != 0 && ordinal != 5) return face;
    vec2 minimum = min(min(face.uvs[0], face.uvs[1]), min(face.uvs[2], face.uvs[3])) * uvDimensions;
    vec2 maximum = max(max(face.uvs[0], face.uvs[1]), max(face.uvs[2], face.uvs[3])) * uvDimensions;
    vec2 expectedMin = ordinal == 0 ? vec2(48.0, 2.0) : vec2(64.0, 18.0);
    vec2 expectedMax = ordinal == 0 ? vec2(48.0, 18.0) : vec2(64.0, 21.0);
    if (any(greaterThan(abs(minimum - expectedMin), vec2(0.001)))
        || any(greaterThan(abs(maximum - expectedMax), vec2(0.001)))) return face;
    vec3 heightEdge = face.corners[0] - face.corners[3];
    vec3 widthDirection = cross(heightEdge, Normal);
    if (dot(widthDirection, widthDirection) <= 1e-20) return face;
    vec3 widthEdge = normalize(widthDirection) * length(heightEdge) / (ordinal == 0 ? 4.0 : 0.75);
    if (ordinal == 0) {
        face.corners[0] += widthEdge;
        face.corners[3] += widthEdge;
    } else {
        face.corners[1] -= widthEdge;
        face.corners[2] -= widthEdge;
    }
    face.valid = true;
    return face;
}
#moj_import <armor3d_equipment.glsl>
#endif

ivec4 armor3dReadMetadata(ivec2 metadataOrigin, int texelOffset) {
    return ivec4(
        texelFetch(Sampler0, metadataOrigin + ivec2(texelOffset, 0), 0)
            * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
    );
}

int armor3dDecodeRow(ivec4 packedRow) {
    return packedRow.r * ARMOR3D_UINT16_BASE + packedRow.g * ARMOR3D_BYTE_BASE + packedRow.b;
}

vec3 armor3dDecodeNormal(ivec4 packedNormal) {
    if (all(equal(packedNormal, ivec4(0)))) return vec3(0.0);
    float encodedX = (packedNormal.r * ARMOR3D_BYTE_BASE + packedNormal.g) * (2.0 / 65535.0) - 1.0;
    float encodedY = (packedNormal.b * ARMOR3D_BYTE_BASE + packedNormal.a) * (2.0 / 65535.0) - 1.0;
    vec3 normal = vec3(encodedX, encodedY, 1.0 - abs(encodedX) - abs(encodedY));
    float fold = max(-normal.z, 0.0);
    normal.x += normal.x >= 0.0 ? -fold : fold;
    normal.y += normal.y >= 0.0 ? -fold : fold;
    return normalize(normal);
}

vec3 armor3dReadNormal(ivec2 dataOrigin, int faceIndex) {
    ivec4 packedNormal = ivec4(texelFetch(Sampler0, dataOrigin + ivec2(faceIndex, 1), 0)
        * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS);
    return armor3dDecodeNormal(packedNormal);
}

vec3 armor3dReadPosition(
    ivec2 dataOrigin,
    int atlasWidth,
    int rowOffset,
    int positionTexelIndex
) {
    uint firstTexelIndex = uint(positionTexelIndex);
    uint width = uint(atlasWidth);
    uint row = firstTexelIndex / width;
    uint column = firstTexelIndex - row * width;
    ivec4 firstTexel = ivec4(
        texelFetch(
            Sampler0,
            dataOrigin + ivec2(int(column), rowOffset + int(row)),
            0
        ) * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
    );
    column++;
    if (column == width) { column = 0u; row++; }
    ivec4 secondTexel = ivec4(
        texelFetch(
            Sampler0,
            dataOrigin + ivec2(int(column), rowOffset + int(row)),
            0
        ) * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
    );
    ivec3 packedPosition = ivec3(
        firstTexel.r * ARMOR3D_POSITION_PACK_FACTOR
            + firstTexel.g * ARMOR3D_NIBBLE_BASE
            + (firstTexel.b >> 4),
        (firstTexel.b & 15) * ARMOR3D_UINT16_BASE
            + firstTexel.a * ARMOR3D_BYTE_BASE
            + secondTexel.r,
        secondTexel.g * ARMOR3D_POSITION_PACK_FACTOR
            + secondTexel.b * ARMOR3D_NIBBLE_BASE
            + (secondTexel.a >> 4)
    );
    return vec3(packedPosition) / ARMOR3D_POSITION_SCALE - vec3(ARMOR3D_POSITION_BIAS);
}

vec2 armor3dReadUv(ivec2 dataOrigin, int atlasWidth, int rowOffset, int uvIndex) {
    ivec4 packedUv = ivec4(
        texelFetch(
            Sampler0,
            dataOrigin + ivec2(uvIndex % atlasWidth, rowOffset + uvIndex / atlasWidth),
            0
        ) * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
    );
    return vec2(
        packedUv.r * ARMOR3D_BYTE_BASE + packedUv.g,
        packedUv.b * ARMOR3D_BYTE_BASE + packedUv.a
    ) / ARMOR3D_UV_NORMALIZATION;
}

ivec2 armor3dReadVertex(ivec2 dataOrigin, int atlasWidth, int rowOffset, int vertexIndex) {
    ivec4 packedVertex = ivec4(
        texelFetch(
            Sampler0,
            dataOrigin + ivec2(vertexIndex % atlasWidth, rowOffset + vertexIndex / atlasWidth),
            0
        ) * ARMOR3D_TEXEL_CHANNEL_SCALE + ARMOR3D_TEXEL_ROUNDING_BIAS
    );
    return ivec2(
        packedVertex.r * ARMOR3D_BYTE_BASE + packedVertex.g,
        packedVertex.b * ARMOR3D_BYTE_BASE + packedVertex.a
    );
}
