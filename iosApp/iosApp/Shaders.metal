#include <metal_stdlib>
using namespace metal;

// ── Cuboid ──────────────────────────────────────────────────────────────────

struct MainUniforms {
    float4x4 uMVP;
    float4x4 uModel;
    float4   uLightPos;            // .xyz = position, .w = unused
    float4   uViewPos;             // .xyz = position, .w = unused
    float4   uLightColorAndGloss;  // .xyz = light colour, .w = materialGloss
};

struct MainVert {
    float3 position [[attribute(0)]];
    float2 texCoord [[attribute(1)]];
    float3 normal   [[attribute(2)]];
};

struct MainVary {
    float4 position [[position]];
    float2 texCoord;
    float3 normal;
    float3 fragPos;
};

vertex MainVary cuboid_vertex(
        MainVert in              [[stage_in]],
        constant MainUniforms& u [[buffer(3)]])
{
    MainVary out;
    out.position = u.uMVP * float4(in.position, 1.0);
    out.texCoord = in.texCoord;
    out.fragPos  = (u.uModel * float4(in.position, 1.0)).xyz;
    float3x3 nm  = float3x3(u.uModel[0].xyz, u.uModel[1].xyz, u.uModel[2].xyz);
    out.normal   = nm * in.normal;
    return out;
}

fragment float4 cuboid_fragment(
        MainVary          in   [[stage_in]],
        texture2d<float>  tex  [[texture(0)]],
        sampler           samp [[sampler(0)]],
        constant MainUniforms& u [[buffer(3)]])
{
    float3 texColor   = tex.sample(samp, in.texCoord).rgb;
    float3 norm       = normalize(in.normal);
    float3 lightDir   = normalize(u.uLightPos.xyz - in.fragPos);
    float3 viewDir    = normalize(u.uViewPos.xyz  - in.fragPos);
    float3 reflectDir = reflect(-lightDir, norm);
    float  diff       = max(dot(norm, lightDir), 0.0);
    float  gloss      = u.uLightColorAndGloss.w;
    float  shininess  = mix(8.0, 128.0, gloss);
    float  specPower  = mix(0.05, 1.0,  gloss);
    float  spec       = pow(max(dot(viewDir, reflectDir), 0.0), shininess) * specPower;
    float3 result     = texColor * (0.4 + 0.6 * diff) + u.uLightColorAndGloss.xyz * spec;
    return float4(result, 1.0);
}

// ── Shadow ───────────────────────────────────────────────────────────────────

struct ShadowUniforms {
    float2 uCenter;
    float2 uScale;
    float  uAlpha; float _pad;
    float2 uSmoothStep;
};

struct ShadowVert {
    float2 position [[attribute(0)]];
};

struct ShadowVary {
    float4 position [[position]];
    float2 vPos;
};

vertex ShadowVary shadow_vertex(
        ShadowVert in              [[stage_in]],
        constant ShadowUniforms& u [[buffer(1)]])
{
    return { float4(in.position, 0.0, 1.0), in.position };
}

fragment float4 shadow_fragment(
        ShadowVary in              [[stage_in]],
        constant ShadowUniforms& u [[buffer(1)]])
{
    float2 rel  = (in.vPos - u.uCenter) / u.uScale;
    float  r    = length(rel);
    float  fade = smoothstep(u.uSmoothStep[0], u.uSmoothStep[1], r);
    return float4(0.0, 0.0, 0.0, (1.0 - fade) * u.uAlpha);
}
