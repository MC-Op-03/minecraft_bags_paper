#version 440
#extension GL_ARB_separate_shader_objects : require

#extension GL_KHR_shader_subgroup_ballot: enable
#extension GL_KHR_shader_subgroup_shuffle: enable
#extension GL_KHR_shader_subgroup_basic: enable
#extension GL_ARB_shader_draw_parameters: enable

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
#include <minecraft:light.glsl>
#endif
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

uniform sampler2D Sampler0;
#if !defined(NO_OVERLAY) && !defined(OIT_ALPHA_ONLY)
uniform sampler2D Sampler1;
#endif
#if !defined(EMISSIVE) && !defined(OIT_ALPHA_ONLY)
uniform sampler2D Sampler2;
#endif

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
layout(location = 2) out vec4 vertexPerFaceColorBack;
layout(location = 3) out vec4 vertexPerFaceColorFront;
#else
layout(location = 2) out vec4 vertexColor;
#endif
layout(location = 4) out vec4 lightColor;
layout(location = 5) out vec4 overlayColor;
layout(location = 6) out vec2 texCoord;
#ifdef GLINT
layout(location = 7) out vec2 texCoordGlint;
#endif
layout(location = 8) out vec2 armor3dNextTexCoord;
layout(location = 9) flat out float armor3dFrameBlend;
layout(location = 10) flat out int armor3dActive;
layout(location = 11) noperspective out float armor3dGeometryInvalid;
layout(location = 12) flat out int armor3dFullbright;
layout(location = 13) flat out vec4 armor3dTint;
layout(location = 14) flat out int armor3dLayerFlags;
layout(location = 15) flat out float armor3dEmissiveMaskOffsetV;
layout(location = 16) flat out float armor3dTintMaskOffsetV;
layout(location = 17) flat out vec4 armor3dAnimationMasks;

#include <minecraft:armor3d_subgroup.glsl>
#include <minecraft:armor3d_tools.glsl>
#include <minecraft:armor3d_animation.glsl>

void main() {
    vec3 transformedPosition = Position;
    vec3 armor3dFaceNormal = Normal;

    texCoord = UV0;
    armor3dTint = Color;
    lightColor = vec4(1);
    overlayColor = vec4(1);
#ifdef APPLY_TEXTURE_MATRIX
    texCoord = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif

#include <minecraft:armor3d_main.glsl>

    if (armor3dHidden) {
        gl_Position = vec4(-3.0, -3.0, -3.0, 1.0);
        return;
    }

#if !defined(NO_OVERLAY) && !defined(OIT_ALPHA_ONLY)
    overlayColor = texelFetch(Sampler1, UV1, 0);
#endif

    if (armor3dActive == 0) {
#ifdef PER_FACE_LIGHTING
        vec2 directionalLight = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
        vertexPerFaceColorBack = minecraft_mix_light_separate(-directionalLight, Color);
        vertexPerFaceColorFront = minecraft_mix_light_separate(directionalLight, Color);
#elif defined(NO_CARDINAL_LIGHTING)
        vertexColor = Color;
#else
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif
    } else {
        vec4 armor3dFaceLight = vec4(1.0);
#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
        if (armor3dFullbright == 0) {
            armor3dFaceLight = minecraft_mix_light(
                Light0_Direction,
                Light1_Direction,
                armor3dFaceNormal,
                overlayColor
            );
        }
#endif
#ifdef PER_FACE_LIGHTING
        vertexPerFaceColorBack = armor3dFaceLight;
        vertexPerFaceColorFront = armor3dFaceLight;
#else
        vertexColor = armor3dFaceLight;
#endif
    }

#if !defined(EMISSIVE) && !defined(OIT_ALPHA_ONLY)
    if (armor3dActive == 0 || armor3dFullbright == 0) {
        lightColor = sample_lightmap(Sampler2, UV2);
    }
#endif

#ifdef GLINT
    texCoordGlint = (TextureMat * vec4(armor3dActive == 1 ? texCoord : UV0, 0.0, 1.0)).xy;
#endif

    gl_Position = ProjMat * ModelViewMat * vec4(transformedPosition, 1.0);
    sphericalVertexDistance = fog_spherical_distance(transformedPosition);
    cylindricalVertexDistance = fog_cylindrical_distance(transformedPosition);
}
