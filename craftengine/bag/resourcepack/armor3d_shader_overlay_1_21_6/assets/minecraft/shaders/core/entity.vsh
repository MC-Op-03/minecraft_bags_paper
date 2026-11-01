#version 410

#extension GL_KHR_shader_subgroup_ballot: enable
#extension GL_KHR_shader_subgroup_shuffle: enable
#extension GL_KHR_shader_subgroup_basic: enable
#extension GL_ARB_shader_draw_parameters: enable

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
#moj_import <minecraft:light.glsl>
#endif
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif
#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
#else
out vec4 vertexColor;
#endif
out vec4 lightColor;
out vec4 overlayColor;
out vec2 texCoord;
out vec2 armor3dNextTexCoord;
flat out float armor3dFrameBlend;
flat out int armor3dActive;
noperspective out float armor3dGeometryInvalid;
flat out int armor3dFullbright;
flat out vec4 armor3dTint;
flat out int armor3dLayerFlags;
flat out float armor3dEmissiveMaskOffsetV;
flat out float armor3dTintMaskOffsetV;
flat out vec4 armor3dAnimationMasks;

#moj_import <armor3d_tools.glsl>
#moj_import <armor3d_animation.glsl>

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

#moj_import <armor3d_main.glsl>

    if (armor3dHidden) {
        gl_Position = vec4(-3.0, -3.0, -3.0, 1.0);
        return;
    }

#ifndef NO_OVERLAY
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

#ifndef EMISSIVE
    if (armor3dActive == 0 || armor3dFullbright == 0) {
        lightColor = texture(Sampler2, vec2(UV2 / 16) / vec2(textureSize(Sampler2, 0)));
    }
#endif

    gl_Position = ProjMat * ModelViewMat * vec4(transformedPosition, 1.0);
    sphericalVertexDistance = fog_spherical_distance(transformedPosition);
    cylindricalVertexDistance = fog_cylindrical_distance(transformedPosition);
}
