bool armor3dUsesNeighbourLookup(int layerType) {
    return layerType == ARMOR3D_LAYER_LLAMA_BODY || layerType == ARMOR3D_LAYER_CAMEL_SADDLE
        || layerType == ARMOR3D_LAYER_CAMEL_HUSK_SADDLE
        || (layerType >= ARMOR3D_LAYER_HORSE_SADDLE && layerType <= ARMOR3D_LAYER_ZOMBIE_HORSE_SADDLE);
}
const ivec4 ARMOR3D_LLAMA_UV[11] = ivec4[](
    ivec4(0, 0, 4, 9),
    ivec4(0, 14, 8, 6),
    ivec4(17, 0, 3, 2),
    ivec4(17, 0, 3, 2),
    ivec4(29, 29, 4, 4),
    ivec4(29, 29, 4, 4),
    ivec4(45, 28, 8, 3),
    ivec4(29, 29, 4, 4),
    ivec4(29, 0, 12, 10),
    ivec4(29, 29, 4, 4),
    ivec4(45, 41, 8, 3)
);
const ivec3 ARMOR3D_LLAMA_TOPOLOGY[11] = ivec3[](
    ivec3(4, 0, 2),
    ivec3(18, 0, 4),
    ivec3(3, 0, 8),
    ivec3(3, 0, 16),
    ivec3(14, 0, 32),
    ivec3(14, 0, 192),
    ivec3(8, 0, 128),
    ivec3(14, 0, 256),
    ivec3(18, 0, 512),
    ivec3(14, 0, 1025),
    ivec3(8, 0, 1)
);

const ivec4 ARMOR3D_EQUINE_UV[19] = ivec4[](
    ivec4(48, 21, 4, 4),
    ivec4(48, 21, 4, 4),
    ivec4(0, 35, 4, 7),
    ivec4(0, 13, 6, 7),
    ivec4(19, 16, 2, 1),
    ivec4(19, 16, 2, 1),
    ivec4(29, 5, 1, 2),
    ivec4(19, 0, 4, 2),
    ivec4(32, 2, 0, 16),
    ivec4(56, 36, 2, 2),
    ivec4(29, 5, 1, 2),
    ivec4(32, 2, 0, 16),
    ivec4(0, 25, 4, 5),
    ivec4(1, 1, 6, 6),
    ivec4(48, 21, 4, 4),
    ivec4(0, 32, 10, 22),
    ivec4(26, 0, 10, 9),
    ivec4(42, 36, 3, 4),
    ivec4(48, 21, 4, 4)
);
const ivec3 ARMOR3D_EQUINE_TOPOLOGY[19] = ivec3[](
    ivec3(11, 0, 2),
    ivec3(11, 0, 4),
    ivec3(12, 0, 8),
    ivec3(5, 0, 16),
    ivec3(3, 0, 32),
    ivec3(3, 0, 64),
    ivec3(2, 0, 128),
    ivec3(5, 0, 768),
    ivec3(3, 0, 512),
    ivec3(16, 0, 1024),
    ivec3(2, 0, 6144),
    ivec3(3, 0, 4096),
    ivec3(5, 0, 8192),
    ivec3(5, 0, 16384),
    ivec3(11, 1, 32768),
    ivec3(10, 0, 65536),
    ivec3(9, 0, 131072),
    ivec3(14, 0, 262144),
    ivec3(11, 1, 1)
);

const ivec4 ARMOR3D_DONKEY_UV[21] = ivec4[](
    ivec4(48, 21, 4, 4),
    ivec4(48, 21, 4, 4),
    ivec4(0, 35, 4, 7),
    ivec4(0, 13, 6, 7),
    ivec4(0, 12, 2, 1),
    ivec4(0, 12, 2, 1),
    ivec4(29, 5, 1, 2),
    ivec4(19, 0, 4, 2),
    ivec4(32, 2, 0, 16),
    ivec4(56, 36, 2, 2),
    ivec4(29, 5, 1, 2),
    ivec4(32, 2, 0, 16),
    ivec4(0, 25, 4, 5),
    ivec4(1, 1, 6, 6),
    ivec4(48, 21, 4, 4),
    ivec4(0, 32, 10, 22),
    ivec4(26, 21, 8, 3),
    ivec4(26, 0, 10, 9),
    ivec4(42, 36, 3, 4),
    ivec4(26, 21, 8, 3),
    ivec4(48, 21, 4, 4)
);
const ivec3 ARMOR3D_DONKEY_TOPOLOGY[21] = ivec3[](
    ivec3(11, 0, 2),
    ivec3(11, 0, 4),
    ivec3(12, 0, 8),
    ivec3(5, 0, 16),
    ivec3(7, 0, 32),
    ivec3(7, 0, 64),
    ivec3(2, 0, 128),
    ivec3(5, 0, 768),
    ivec3(3, 0, 512),
    ivec3(16, 0, 1024),
    ivec3(2, 0, 6144),
    ivec3(3, 0, 4096),
    ivec3(5, 0, 8192),
    ivec3(5, 0, 16384),
    ivec3(11, 1, 32768),
    ivec3(10, 0, 65536),
    ivec3(8, 0, 131072),
    ivec3(9, 0, 262144),
    ivec3(14, 0, 524288),
    ivec3(8, 0, 1048576),
    ivec3(11, 1, 1)
);

const ivec4 ARMOR3D_CAMEL_UV[23] = ivec4[](
    ivec4(0, 26, 5, 5),
    ivec4(94, 16, 5, 5),
    ivec4(58, 16, 5, 5),
    ivec4(0, 25, 15, 27),
    ivec4(60, 24, 7, 19),
    ivec4(21, 0, 7, 7),
    ivec4(50, 0, 5, 6),
    ivec4(60, 87, 7, 19),
    ivec4(21, 64, 7, 7),
    ivec4(50, 64, 5, 6),
    ivec4(74, 70, 1, 2),
    ivec4(74, 70, 1, 2),
    ivec4(67, 0, 3, 2),
    ivec4(98, 42, 0, 15),
    ivec4(84, 57, 7, 0),
    ivec4(98, 42, 0, 15),
    ivec4(45, 0, 3, 2),
    ivec4(74, 0, 9, 11),
    ivec4(74, 64, 9, 11),
    ivec4(92, 114, 7, 11),
    ivec4(0, 89, 15, 27),
    ivec4(122, 0, 3, 0),
    ivec4(0, 0, 5, 5)
);
const ivec3 ARMOR3D_CAMEL_TOPOLOGY[23] = ivec3[](
    ivec3(21, 0, 2),
    ivec3(21, 0, 4),
    ivec3(21, 0, 8),
    ivec3(12, 0, 16),
    ivec3(8, 0, 32),
    ivec3(14, 0, 64),
    ivec3(5, 0, 128),
    ivec3(8, 0, 256),
    ivec3(14, 0, 512),
    ivec3(5, 0, 1024),
    ivec3(2, 0, 2048),
    ivec3(2, 1, 4096),
    ivec3(1, 0, 73728),
    ivec3(7, 0, 16384),
    ivec3(7, 0, 32768),
    ivec3(7, 0, 65536),
    ivec3(1, 0, 131072),
    ivec3(5, 0, 262144),
    ivec3(5, 0, 524288),
    ivec3(3, 0, 1048576),
    ivec3(12, 0, 2097152),
    ivec3(14, 0, 4194304),
    ivec3(21, 0, 1)
);

int armor3dCarrierFamily(int layerType) {
    if (layerType == ARMOR3D_LAYER_LLAMA_BODY) return 0;
    if (layerType == ARMOR3D_LAYER_DONKEY_SADDLE || layerType == ARMOR3D_LAYER_MULE_SADDLE) return 2;
    if (layerType == ARMOR3D_LAYER_CAMEL_SADDLE || layerType == ARMOR3D_LAYER_CAMEL_HUSK_SADDLE) return 3;
    return 1;
}

ivec4 armor3dTopologyUv(int family, int index) {
    if (family == 0) return ARMOR3D_LLAMA_UV[index];
    if (family == 1) return ARMOR3D_EQUINE_UV[index];
    if (family == 2) return ARMOR3D_DONKEY_UV[index];
    return ARMOR3D_CAMEL_UV[index];
}

ivec3 armor3dTopologyInfo(int family, int index) {
    if (family == 0) return ARMOR3D_LLAMA_TOPOLOGY[index];
    if (family == 1) return ARMOR3D_EQUINE_TOPOLOGY[index];
    if (family == 2) return ARMOR3D_DONKEY_TOPOLOGY[index];
    return ARMOR3D_CAMEL_TOPOLOGY[index];
}

uint armor3dBoundaryCandidates(int family, int count, int face, vec2 uvMin, vec2 uvMax, bool mirror) {
    uint candidates = 0u;
    for (int index = 0; index < count; ++index) {
        ivec4 box = armor3dTopologyUv(family, index);
        ivec3 info = armor3dTopologyInfo(family, index);
        vec2 minimum = face == 0 ? vec2(box.x + box.w, box.y)
            : vec2(box.x + 2 * box.w + box.z, box.y + box.w);
        vec2 maximum = minimum + vec2(box.z, face == 0 ? box.w : info.x);
        if (all(lessThan(abs(uvMin - minimum), vec2(0.001)))
            && all(lessThan(abs(uvMax - maximum), vec2(0.001))) && mirror == (info.y != 0)) {
            candidates |= 1u << uint(index);
        }
    }
    return candidates;
}

bool armor3dTopologyFollows(int family, int before, int after, bool lateral) {
    if ((uint(armor3dTopologyInfo(family, before).z) & (1u << uint(after))) == 0u) return false;
    if (family == 0 && (before == 4 || before == 5) && (after == 5 || after == 7)) {
        return lateral == (before == 5 && after == 7);
    }
    return true;
}

uint armor3dFilterNeighbours(int family, uint candidates, uint neighbour, bool previous, bool lateral) {
    uint filtered = 0u;
    for (uint remaining = candidates; remaining != 0u; remaining &= remaining - 1u) {
        int index = findLSB(remaining);
        for (uint other = neighbour; other != 0u; other &= other - 1u) {
            int next = findLSB(other);
            if (previous ? armor3dTopologyFollows(family, next, index, lateral)
                         : armor3dTopologyFollows(family, index, next, lateral)) {
                filtered |= 1u << uint(index);
                break;
            }
        }
    }
    return filtered;
}

uint armor3dFaceCandidates(int family, int count, int cubeFace, vec2 uvDimensions, Armor3dFace face) {
    uint candidates = 0u;
    if (face.valid && gl_SubgroupInvocationID == face.lanes[0] && (cubeFace == 0 || cubeFace == 5)) {
        vec2 minimum = min(min(face.uvs[0], face.uvs[1]), min(face.uvs[2], face.uvs[3]));
        vec2 maximum = max(max(face.uvs[0], face.uvs[1]), max(face.uvs[2], face.uvs[3]));
        candidates = armor3dBoundaryCandidates(family, count, cubeFace, minimum * uvDimensions,
            maximum * uvDimensions, face.uvs[0].y > face.uvs[3].y);
    }
    return subgroupShuffle(candidates, face.valid ? face.lanes[0] : gl_SubgroupInvocationID);
}

int armor3dNeighbourCarrier(int layerType, int count, vec2 uvDimensions, Armor3dFace face, out int geometryFace) {
    geometryFace = 5;
    if (gl_SubgroupSize < 16u) return -1;
    int family = armor3dCarrierFamily(layerType);
    int localVertex = ARMOR3D_VERTEX_ID - gl_BaseVertexARB;
    int cubeBase = ARMOR3D_VERTEX_ID - localVertex % 24;
    int cubeFace = (localVertex % 24) / 4;
    geometryFace = cubeFace;
    uint candidates = armor3dFaceCandidates(family, count, cubeFace, uvDimensions, face);
    uint originalCandidates = candidates;
    vec3 center = (face.corners[0] + face.corners[1] + face.corners[2] + face.corners[3]) * 0.25;
    vec3 xEdge = face.corners[0] - face.corners[1];
    float widthSquared = dot(xEdge, xEdge);
    uvec4 activeLanes = subgroupBallot(true);
    bool completeFaces = subgroupBallotBitCount(subgroupBallot(!face.valid)) == 0u;
    uvec4 identityLanes = subgroupBallot(!completeFaces || (localVertex & 3) == 0);
    uvec4 boundaryLanes = subgroupBallot((localVertex & 3) == 0 && originalCandidates != 0u);
    int minimumVertex = ARMOR3D_VERTEX_ID;
    int maximumVertex = ARMOR3D_VERTEX_ID;
    for (uint lane = 0u; lane < gl_SubgroupSize; ++lane) {
        if (subgroupBallotBitExtract(identityLanes, lane)) {
            ivec3 identity = subgroupShuffle(ivec3(ARMOR3D_VERTEX_ID, ARMOR3D_INSTANCE_ID, gl_DrawIDARB), lane);
            if (identity.y == ARMOR3D_INSTANCE_ID && identity.z == gl_DrawIDARB) {
                minimumVertex = min(minimumVertex, identity.x);
                maximumVertex = max(maximumVertex, identity.x + (completeFaces ? 3 : 0));
            }
            if (subgroupBallotBitExtract(boundaryLanes, lane)) {
                uint neighbour = subgroupShuffle(originalCandidates, lane);
                vec3 otherCenter = subgroupShuffle(center, lane);
                int otherLocal = identity.x - gl_BaseVertexARB;
                int otherBase = identity.x - otherLocal % 24;
                if (identity.y == ARMOR3D_INSTANCE_ID && identity.z == gl_DrawIDARB
                    && (otherLocal & 3) == 0 && abs(otherBase - cubeBase) == 24) {
                    float lateralDistance = dot(otherCenter - center, xEdge);
                    bool lateral = lateralDistance * lateralDistance > widthSquared * widthSquared * 0.25;
                    candidates = armor3dFilterNeighbours(family, candidates, neighbour, otherBase < cubeBase, lateral);
                }
            }
        }
    }
    if (cubeBase == gl_BaseVertexARB) candidates &= 1u;
    if (bitCount(candidates) > 1 && subgroupBallotBitCount(activeLanes) < gl_SubgroupSize && maximumVertex == cubeBase + 23) {
        uint endings = 0u;
        for (uint remaining = candidates; remaining != 0u; remaining &= remaining - 1u) {
            int index = findLSB(remaining);
            if ((armor3dTopologyInfo(family, index).z & 1) != 0) endings |= 1u << uint(index);
        }
        candidates = endings;
    }
    uint contextMask = family == 0 ? 688u : family == 1 ? 280835u : family == 2 ? 1067267u : 0u;
    bool needsContext = cubeBase != gl_BaseVertexARB && (originalCandidates & contextMask) != 0u;
    bool firstBoundary = gl_SubgroupSize == 16u && needsContext && cubeBase == minimumVertex + 8;
    bool isolatedTail = gl_SubgroupSize == 16u && needsContext && cubeBase == minimumVertex - 8;
    bool emitFace = firstBoundary ? cubeFace == 0 : cubeFace == 5 && !isolatedTail;
    return emitFace && bitCount(candidates) == 1 ? findLSB(candidates) : -1;
}
