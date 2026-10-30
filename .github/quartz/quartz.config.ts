import { QuartzConfig } from "./quartz/cfg"
import * as Plugin from "./quartz/plugins"

/**
 * Quartz 4.0 Configuration for NeuroAI Knowledge Base
 * Live URL: https://airwar31.github.io/neuro-AI/
 */
const config: QuartzConfig = {
  configuration: {
    pageTitle: "🧠 NeuroAI Knowledge Base",
    pageTitleSuffix: " | NeuroAI",
    enableSPA: true,
    enablePopovers: true,
    analytics: null,
    locale: "ru-RU",
    baseUrl: "airwar31.github.io/neuro-AI",
    ignorePatterns: ["private", "templates", ".obsidian"],
    defaultDateType: "created",
    theme: {
      fontOrigin: "googleFonts",
      cdnCaching: true,
      typography: {
        header: "Inter",
        body: "Source Sans Pro",
        code: "IBM Plex Mono",
      },
      colors: {
        lightMode: {
          light: "#f8fafc",
          lightgray: "#e2e8f0",
          gray: "#94a3b8",
          darkgray: "#334155",
          dark: "#0f172a",
          secondary: "#7c3aed",
          tertiary: "#0ea5e9",
          highlight: "rgba(124, 58, 237, 0.12)",
          textHighlight: "#fef08a88",
        },
        darkMode: {
          light: "#0b0f19",
          lightgray: "#1e293b",
          gray: "#64748b",
          darkgray: "#cbd5e1",
          dark: "#f8fafc",
          secondary: "#a78bfa",
          tertiary: "#38bdf8",
          highlight: "rgba(167, 139, 250, 0.15)",
          textHighlight: "#a78bfa44",
        },
      },
    },
  },
  plugins: {
    transformers: [
      Plugin.FrontMatter(),
      Plugin.CreatedModifiedDate({
        priority: ["frontmatter", "filesystem"],
      }),
      Plugin.Latex({ renderEngine: "katex" }),
      Plugin.SyntaxHighlighting({
        theme: {
          light: "github-light",
          dark: "github-dark",
        },
        keepBackground: false,
      }),
      Plugin.ObsidianFlavoredMarkdown({ enableInHtmlEmbed: false }),
      Plugin.GitHubFlavoredMarkdown(),
      Plugin.TableOfContents(),
      Plugin.CrawlLinks({ markdownLinkResolution: "shortest" }),
      Plugin.Description(),
    ],
    filters: [Plugin.RemoveDrafts()],
    emitters: [
      Plugin.AliasRedirects(),
      Plugin.ComponentPage(),
      Plugin.ContentPage(),
      Plugin.FolderPage(),
      Plugin.TagPage(),
      Plugin.ContentIndex({
        enableSiteMap: true,
        enableRSS: true,
      }),
      Plugin.Assets(),
      Plugin.Static(),
      Plugin.NotFoundPage(),
    ],
  },
}

export default config

