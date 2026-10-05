# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Generates the configured architecture deliverables and presentation exports from the repository model/document sources while preserving native model inputs.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
param(
    [string] $OutputDirectory = $PSScriptRoot + '\..'
)

$ErrorActionPreference = 'Stop'
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$imageDirectory = Join-Path $OutputDirectory 'diagrams'
New-Item -ItemType Directory -Path $imageDirectory -Force | Out-Null

Add-Type -AssemblyName System.Drawing
$rendererSource = @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Globalization;
using System.Text.RegularExpressions;
using System.Xml;

public static class PortageSvgRasterizer
{
    private sealed class Style
    {
        public string Fill = "#000000";
        public string Stroke = "none";
        public float StrokeWidth = 1;
        public float FontSize = 14;
        public string FontFamily = "Arial";
        public string FontWeight = "normal";
        public string TextAnchor = "start";
        public bool IsDashed;

        public Style Copy()
        {
            return (Style)MemberwiseClone();
        }
    }

    private static float Number(string value, float fallback)
    {
        float result;
        return Single.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out result) ? result : fallback;
    }

    private static Style ChildStyle(XmlElement element, Style parent)
    {
        Style style = parent.Copy();
        string inline = element.GetAttribute("style");
        Dictionary<string, string> properties = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (Match match in Regex.Matches(inline, @"([^:;]+):([^;]+)"))
            properties[match.Groups[1].Value.Trim()] = match.Groups[2].Value.Trim();
        string value;
        if (element.HasAttribute("fill")) style.Fill = element.GetAttribute("fill");
        if (element.HasAttribute("stroke")) style.Stroke = element.GetAttribute("stroke");
        if (element.HasAttribute("stroke-width")) style.StrokeWidth = Number(element.GetAttribute("stroke-width"), style.StrokeWidth);
        if (element.HasAttribute("font-family")) style.FontFamily = element.GetAttribute("font-family").Split(',')[0].Trim(' ', '\'', '"');
        if (element.HasAttribute("font-size")) style.FontSize = Number(element.GetAttribute("font-size").Replace("px", ""), style.FontSize);
        if (element.HasAttribute("font-weight")) style.FontWeight = element.GetAttribute("font-weight");
        if (element.HasAttribute("text-anchor")) style.TextAnchor = element.GetAttribute("text-anchor");
        if (element.HasAttribute("stroke-dasharray")) style.IsDashed = true;
        if (properties.TryGetValue("fill", out value)) style.Fill = value;
        if (properties.TryGetValue("stroke", out value)) style.Stroke = value;
        if (properties.TryGetValue("stroke-width", out value)) style.StrokeWidth = Number(value, style.StrokeWidth);
        if (properties.TryGetValue("font-family", out value)) style.FontFamily = value.Split(',')[0].Trim(' ', '\'', '"');
        if (properties.TryGetValue("font-size", out value)) style.FontSize = Number(value.Replace("px", "").Replace("pt", ""), style.FontSize);
        if (properties.TryGetValue("font-weight", out value)) style.FontWeight = value;
        if (properties.TryGetValue("text-anchor", out value)) style.TextAnchor = value;
        if (properties.ContainsKey("stroke-dasharray")) style.IsDashed = true;
        return style;
    }

    private static Color ColorValue(string value)
    {
        if (String.IsNullOrWhiteSpace(value) || value.Equals("none", StringComparison.OrdinalIgnoreCase))
            return Color.Transparent;
        if (value.StartsWith("rgb(", StringComparison.OrdinalIgnoreCase))
        {
            Match m = Regex.Match(value, @"rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)");
            if (m.Success) return Color.FromArgb(Int32.Parse(m.Groups[1].Value), Int32.Parse(m.Groups[2].Value), Int32.Parse(m.Groups[3].Value));
        }
        if (value.StartsWith("#") && value.Length == 4)
            return Color.FromArgb(Convert.ToInt32(new String(value[1], 2), 16), Convert.ToInt32(new String(value[2], 2), 16), Convert.ToInt32(new String(value[3], 2), 16));
        if (value.StartsWith("#") && value.Length == 7)
            return ColorTranslator.FromHtml(value);
        try { return Color.FromName(value); }
        catch { return Color.Black; }
    }

    private static void PaintFill(Graphics graphics, string color, Action<Brush> paint)
    {
        Color c = ColorValue(color);
        if (c.A == 0) return;
        using (Brush brush = new SolidBrush(c)) paint(brush);
    }

    private static void PaintStroke(Graphics graphics, Style style, Action<Pen> paint)
    {
        Color c = ColorValue(style.Stroke);
        if (c.A == 0) return;
        using (Pen pen = new Pen(c, Math.Max(0.6f, style.StrokeWidth)))
        {
            pen.LineJoin = LineJoin.Round;
            pen.StartCap = LineCap.Round;
            pen.EndCap = LineCap.Round;
            if (style.IsDashed) pen.DashStyle = DashStyle.Dash;
            paint(pen);
        }
    }

    private static float Attr(XmlElement e, string name, float fallback)
    {
        return e.HasAttribute(name) ? Number(e.GetAttribute(name), fallback) : fallback;
    }

    public static byte[] Render(string svg, int scale)
    {
        XmlDocument document = new XmlDocument();
        document.XmlResolver = null;
        document.LoadXml(svg);
        XmlElement root = document.DocumentElement;
        string[] viewBox = Regex.Split(root.GetAttribute("viewBox").Trim(), @"\s+");
        float x = viewBox.Length == 4 ? Number(viewBox[0], 0) : 0;
        float y = viewBox.Length == 4 ? Number(viewBox[1], 0) : 0;
        float width = viewBox.Length == 4 ? Number(viewBox[2], Number(root.GetAttribute("width"), 960)) : Number(root.GetAttribute("width"), 960);
        float height = viewBox.Length == 4 ? Number(viewBox[3], Number(root.GetAttribute("height"), 480)) : Number(root.GetAttribute("height"), 480);
        int pixelWidth = Math.Max(1, (int)Math.Ceiling(width * scale));
        int pixelHeight = Math.Max(1, (int)Math.Ceiling(height * scale));
        using (Bitmap bitmap = new Bitmap(pixelWidth, pixelHeight))
        using (Graphics graphics = Graphics.FromImage(bitmap))
        using (System.IO.MemoryStream stream = new System.IO.MemoryStream())
        {
            graphics.SmoothingMode = SmoothingMode.AntiAlias;
            graphics.TextRenderingHint = System.Drawing.Text.TextRenderingHint.AntiAliasGridFit;
            graphics.Clear(Color.White);
            graphics.ScaleTransform(scale, scale);
            graphics.TranslateTransform(-x, -y);
            DrawChildren(root, new Style(), graphics);
            bitmap.Save(stream, System.Drawing.Imaging.ImageFormat.Png);
            return stream.ToArray();
        }
    }

    private static void DrawChildren(XmlElement parent, Style inherited, Graphics graphics)
    {
        foreach (XmlNode node in parent.ChildNodes)
        {
            XmlElement element = node as XmlElement;
            if (element == null) continue;
            string kind = element.LocalName;
            Style style = ChildStyle(element, inherited);
            if (kind == "defs" || kind == "marker" || kind == "title" || kind == "desc") continue;
            float x = Attr(element, "x", 0), y = Attr(element, "y", 0);
            float w = Attr(element, "width", 0), h = Attr(element, "height", 0);
            if (kind == "g")
            {
                DrawChildren(element, style, graphics);
            }
            else if (kind == "rect")
            {
                RectangleF r = new RectangleF(x, y, w, h);
                float radius = Attr(element, "rx", 0);
                if (radius > 0)
                {
                    using (GraphicsPath p = new GraphicsPath())
                    {
                        float d = radius * 2;
                        p.AddArc(x, y, d, d, 180, 90); p.AddArc(x + w - d, y, d, d, 270, 90);
                        p.AddArc(x + w - d, y + h - d, d, d, 0, 90); p.AddArc(x, y + h - d, d, d, 90, 90); p.CloseFigure();
                        PaintFill(graphics, style.Fill, b => graphics.FillPath(b, p));
                        PaintStroke(graphics, style, pen => graphics.DrawPath(pen, p));
                    }
                }
                else
                {
                    PaintFill(graphics, style.Fill, b => graphics.FillRectangle(b, r));
                    PaintStroke(graphics, style, pen => graphics.DrawRectangle(pen, x, y, w, h));
                }
            }
            else if (kind == "line")
            {
                float x1 = Attr(element, "x1", 0), y1 = Attr(element, "y1", 0);
                float x2 = Attr(element, "x2", 0), y2 = Attr(element, "y2", 0);
                PaintStroke(graphics, style, pen => graphics.DrawLine(pen, x1, y1, x2, y2));
            }
            else if (kind == "circle" || kind == "ellipse")
            {
                float cx = Attr(element, "cx", 0), cy = Attr(element, "cy", 0);
                float rx = kind == "circle" ? Attr(element, "r", 0) : Attr(element, "rx", 0);
                float ry = kind == "circle" ? rx : Attr(element, "ry", 0);
                RectangleF r = new RectangleF(cx - rx, cy - ry, 2 * rx, 2 * ry);
                PaintFill(graphics, style.Fill, b => graphics.FillEllipse(b, r));
                PaintStroke(graphics, style, pen => graphics.DrawEllipse(pen, r));
            }
            else if (kind == "polygon" || kind == "polyline")
            {
                MatchCollection numbers = Regex.Matches(element.GetAttribute("points"), @"[-+]?(?:\d*\.?\d+)(?:[eE][-+]?\d+)?");
                List<PointF> points = new List<PointF>();
                for (int i = 0; i + 1 < numbers.Count; i += 2)
                    points.Add(new PointF(Number(numbers[i].Value, 0), Number(numbers[i + 1].Value, 0)));
                if (points.Count > 1)
                {
                    if (kind == "polygon") PaintFill(graphics, style.Fill, b => graphics.FillPolygon(b, points.ToArray()));
                    PaintStroke(graphics, style, pen => graphics.DrawLines(pen, points.ToArray()));
                }
            }
            else if (kind == "path")
            {
                List<PointF> points = ParsePath(element.GetAttribute("d"));
                if (points.Count > 1)
                    PaintStroke(graphics, style, pen => graphics.DrawLines(pen, points.ToArray()));
            }
            else if (kind == "text")
            {
                float size = Math.Max(6, style.FontSize);
                FontStyle fontStyle = style.FontWeight.Equals("bold", StringComparison.OrdinalIgnoreCase) || Number(style.FontWeight, 400) >= 600 ? FontStyle.Bold : FontStyle.Regular;
                using (Font font = new Font(style.FontFamily, size, fontStyle, GraphicsUnit.Pixel))
                {
                    string text = element.InnerText;
                    SizeF measured = graphics.MeasureString(text, font);
                    float left = style.TextAnchor == "middle" ? x - measured.Width / 2 : style.TextAnchor == "end" ? x - measured.Width : x;
                    PaintFill(graphics, style.Fill, b => graphics.DrawString(text, font, b, left, y - size * 0.86f));
                }
            }
            if ((kind == "rect" || kind == "line" || kind == "circle" || kind == "ellipse" || kind == "polygon" || kind == "polyline" || kind == "path" || kind == "text") && element.HasChildNodes)
                DrawChildren(element, style, graphics);
        }
    }

    private static List<PointF> ParsePath(string data)
    {
        MatchCollection matches = Regex.Matches(data, @"[A-Za-z]|[-+]?(?:\d*\.?\d+)(?:[eE][-+]?\d+)?");
        List<PointF> result = new List<PointF>();
        float cx = 0, cy = 0, sx = 0, sy = 0;
        char command = ' ';
        int i = 0;
        while (i < matches.Count)
        {
            string token = matches[i].Value;
            if (Regex.IsMatch(token, @"^[A-Za-z]$"))
            {
                command = token[0]; i++;
                if (command == 'Z' || command == 'z') { cx = sx; cy = sy; continue; }
            }
            bool relative = Char.IsLower(command);
            char op = Char.ToUpperInvariant(command);
            if (op == 'M' || op == 'L')
            {
                if (i + 1 >= matches.Count) break;
                float nx = Number(matches[i++].Value, 0), ny = Number(matches[i++].Value, 0);
                cx = relative ? cx + nx : nx; cy = relative ? cy + ny : ny;
                result.Add(new PointF(cx, cy));
                if (op == 'M') { sx = cx; sy = cy; command = relative ? 'l' : 'L'; }
            }
            else if (op == 'H' && i < matches.Count)
            {
                float n = Number(matches[i++].Value, 0); cx = relative ? cx + n : n; result.Add(new PointF(cx, cy));
            }
            else if (op == 'V' && i < matches.Count)
            {
                float n = Number(matches[i++].Value, 0); cy = relative ? cy + n : n; result.Add(new PointF(cx, cy));
            }
            else if (op == 'C' && i + 5 < matches.Count)
            {
                float x1 = Number(matches[i++].Value, 0), y1 = Number(matches[i++].Value, 0);
                float x2 = Number(matches[i++].Value, 0), y2 = Number(matches[i++].Value, 0);
                float x3 = Number(matches[i++].Value, 0), y3 = Number(matches[i++].Value, 0);
                if (relative) { x1 += cx; x2 += cx; x3 += cx; y1 += cy; y2 += cy; y3 += cy; }
                float x0 = cx, y0 = cy;
                for (int step = 1; step <= 16; step++)
                {
                    float t = step / 16f, mt = 1 - t;
                    float px = mt * mt * mt * x0 + 3 * mt * mt * t * x1 + 3 * mt * t * t * x2 + t * t * t * x3;
                    float py = mt * mt * mt * y0 + 3 * mt * mt * t * y1 + 3 * mt * t * t * y2 + t * t * t * y3;
                    result.Add(new PointF(px, py));
                }
                cx = x3; cy = y3;
            }
            else { i++; }
        }
        return result;
    }
}
'@
Add-Type -TypeDefinition $rendererSource -ReferencedAssemblies @('System.Drawing', 'System.Xml')
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Convert-TableHtmlToParagraphs {
    param([string] $Html)
    $tablePattern = [regex]::new('<table\b.*?</table>', [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    $tableMatches = $tablePattern.Matches($Html)
    $builder = New-Object System.Text.StringBuilder
    $cursor = 0
    foreach ($table in $tableMatches) {
        [void]$builder.Append($Html.Substring($cursor, $table.Index - $cursor))
        $rows = [regex]::Matches($table.Value, '<tr\b[^>]*>(.*?)</tr>', [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        $headerCells = @()
        if ($rows.Count -gt 0) {
            $headerCells = [regex]::Matches($rows[0].Groups[1].Value, '<t[hd]\b[^>]*>(.*?)</t[hd]>', [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        }
        if ($headerCells.Count -gt 0) {
            $headerText = @()
            foreach ($cell in $headerCells) {
                $text = [regex]::Replace($cell.Groups[1].Value, '<[^>]+>', ' ')
                $headerText += ([System.Net.WebUtility]::HtmlDecode($text) -replace '\s+', ' ').Trim()
            }
            [void]$builder.Append('<p><b>')
            [void]$builder.Append([System.Net.WebUtility]::HtmlEncode(($headerText -join ' | ')))
            [void]$builder.Append('</b></p>')
        }
        for ($rowIndex = 1; $rowIndex -lt $rows.Count; $rowIndex++) {
            $cells = [regex]::Matches($rows[$rowIndex].Groups[1].Value, '<t[hd]\b[^>]*>(.*?)</t[hd]>', [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
            $labels = @()
            for ($cellIndex = 0; $cellIndex -lt $cells.Count; $cellIndex++) {
                $raw = [regex]::Replace($cells[$cellIndex].Groups[1].Value, '<[^>]+>', ' ')
                $value = ([System.Net.WebUtility]::HtmlDecode($raw) -replace '\s+', ' ').Trim()
                if ($value.Length -gt 0) {
                    $label = if ($cellIndex -lt $headerText.Count) { $headerText[$cellIndex] } else { "Field $($cellIndex + 1)" }
                    $labels += ('<b>' + [System.Net.WebUtility]::HtmlEncode($label) + ':</b> ' + [System.Net.WebUtility]::HtmlEncode($value))
                }
            }
            [void]$builder.Append('<p>')
            [void]$builder.Append(($labels -join ' &nbsp; '))
            [void]$builder.Append('</p>')
        }
        $cursor = $table.Index + $table.Length
    }
    [void]$builder.Append($Html.Substring($cursor))
    return $builder.ToString()
}

function Add-ZipTextEntry {
    param($Archive, [string] $EntryPath, [string] $Contents)
    $entry = $Archive.CreateEntry($EntryPath)
    $entryStream = $entry.Open()
    $writer = [System.IO.StreamWriter]::new($entryStream, [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write($Contents)
    } finally {
        $writer.Dispose()
    }
}

function Add-ZipFileEntry {
    param($Archive, [string] $EntryPath, [string] $FilePath)
    $entry = $Archive.CreateEntry($EntryPath)
    $entryStream = $entry.Open()
    $fileStream = [System.IO.File]::OpenRead($FilePath)
    try {
        $fileStream.CopyTo($entryStream)
    } finally {
        $fileStream.Dispose()
        $entryStream.Dispose()
    }
}

function ConvertTo-WordXmlText {
    param([string] $Text)
    $validText = [regex]::Replace($Text, '[\u0000-\u0008\u000B\u000C\u000E-\u001F]', '')
    return [System.Security.SecurityElement]::Escape($validText)
}

function New-OpenXmlDocument {
    param(
        [string] $DocumentPath,
        [string] $BaseName,
        [string] $ImageDirectory,
        [object[]] $Paragraphs,
        [string[]] $FigureCaptions
    )

    $documentXml = New-Object System.Text.StringBuilder
    [void]$documentXml.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
    [void]$documentXml.Append('<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>')
    foreach ($paragraph in $Paragraphs) {
        [void]$documentXml.Append('<w:p>')
        if ($paragraph.PageBreak) {
            [void]$documentXml.Append('<w:pPr><w:pageBreakBefore/></w:pPr>')
        } elseif ($paragraph.Style) {
            [void]$documentXml.Append("<w:pPr><w:pStyle w:val=""$($paragraph.Style)""/></w:pPr>")
        }
        [void]$documentXml.Append('<w:r><w:t xml:space="preserve">')
        [void]$documentXml.Append((ConvertTo-WordXmlText $paragraph.Text))
        [void]$documentXml.Append('</w:t></w:r></w:p>')
    }

    [void]$documentXml.Append('<w:p><w:pPr><w:pStyle w:val="Heading1"/><w:pageBreakBefore/></w:pPr><w:r><w:t>Architecture diagrams</w:t></w:r></w:p>')
    $imageRelationships = New-Object System.Text.StringBuilder
    for ($figureIndex = 1; $figureIndex -le $FigureCaptions.Count; $figureIndex++) {
        $figureNumber = '{0:D2}' -f $figureIndex
        $imagePath = Join-Path $ImageDirectory "$BaseName-Figure-$figureNumber.png"
        $image = [System.Drawing.Image]::FromFile($imagePath)
        try {
            $extentX = [int64][Math]::Min(5486400, ($image.Width * 9525))
            $extentY = [int64][Math]::Round($extentX * $image.Height / $image.Width)
        } finally {
            $image.Dispose()
        }
        $relationshipId = "rIdImage$figureIndex"
        $escapedFileName = ConvertTo-WordXmlText "$BaseName-Figure-$figureNumber.png"
        $escapedCaption = ConvertTo-WordXmlText "Figure $figureIndex - $($FigureCaptions[$figureIndex - 1])"
        [void]$documentXml.Append("<w:p><w:pPr><w:pStyle w:val=""Caption""/></w:pPr><w:r><w:t>$escapedCaption</w:t></w:r></w:p>")
        [void]$documentXml.Append("<w:p><w:pPr><w:jc w:val=""center""/></w:pPr><w:r><w:drawing><wp:inline distT=""0"" distB=""0"" distL=""0"" distR=""0""><wp:extent cx=""$extentX"" cy=""$extentY""/><wp:docPr id=""$figureIndex"" name=""$escapedFileName"" descr=""$escapedCaption""/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=""1""/></wp:cNvGraphicFramePr><a:graphic><a:graphicData uri=""http://schemas.openxmlformats.org/drawingml/2006/picture""><pic:pic><pic:nvPicPr><pic:cNvPr id=""$figureIndex"" name=""$escapedFileName""/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed=""$relationshipId""/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x=""0"" y=""0""/><a:ext cx=""$extentX"" cy=""$extentY""/></a:xfrm><a:prstGeom prst=""rect""><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>")
        [void]$imageRelationships.Append("<Relationship Id=""$relationshipId"" Type=""http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"" Target=""media/image$figureIndex.png""/>")
    }
    [void]$documentXml.Append('<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="900" w:right="900" w:bottom="900" w:left="900" w:header="450" w:footer="450" w:gutter="0"/></w:sectPr></w:body></w:document>')

    $contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>
'@
    $rootRelationships = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@
    $documentRelationships = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  $($imageRelationships.ToString())
</Relationships>
"@
    $styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="20"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="100" w:line="276" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="20"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/></w:pPr><w:rPr><w:b/><w:color w:val="17456B"/><w:sz w:val="34"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="180" w:after="80"/></w:pPr><w:rPr><w:b/><w:color w:val="17456B"/><w:sz w:val="28"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="140" w:after="60"/></w:pPr><w:rPr><w:b/><w:color w:val="1F5A79"/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Caption"><w:name w:val="caption"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:i/><w:color w:val="56697A"/><w:sz w:val="18"/></w:rPr></w:style>
</w:styles>
'@

    if (Test-Path -LiteralPath $DocumentPath) {
        Remove-Item -LiteralPath $DocumentPath -Force
    }
    $archive = [System.IO.Compression.ZipFile]::Open($DocumentPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Add-ZipTextEntry $archive '[Content_Types].xml' $contentTypes
        Add-ZipTextEntry $archive '_rels/.rels' $rootRelationships
        Add-ZipTextEntry $archive 'word/document.xml' $documentXml.ToString()
        Add-ZipTextEntry $archive 'word/styles.xml' $styles
        Add-ZipTextEntry $archive 'word/_rels/document.xml.rels' $documentRelationships
        for ($figureIndex = 1; $figureIndex -le $FigureCaptions.Count; $figureIndex++) {
            $figureNumber = '{0:D2}' -f $figureIndex
            $imagePath = Join-Path $ImageDirectory "$BaseName-Figure-$figureNumber.png"
            Add-ZipFileEntry $archive "word/media/image$figureIndex.png" $imagePath
        }
    } finally {
        $archive.Dispose()
    }
}

$statusPath = Join-Path $env:TEMP 'PortageCyberTech-architecture-doc-status.txt'
$profilePath = $null
function Set-ConversionStatus {
    param([string] $Status)
    [System.IO.File]::WriteAllText($statusPath, $Status)
}
try {
    $reportsRoot = Split-Path -Parent $OutputDirectory
    $documents = @(
        @{
            SourcePath = Join-Path $OutputDirectory 'fr\dossier-architecture-portage-cybertech.html'
            OutputDirectory = Join-Path $OutputDirectory 'fr'
            BaseName = 'dossier-architecture-portage-cybertech'
        },
        @{
            SourcePath = Join-Path $reportsRoot '01_Cadrage\fr\cahier-des-charges-fonctionnel-technique.html'
            OutputDirectory = Join-Path $reportsRoot '01_Cadrage\fr'
            BaseName = 'cahier-des-charges-fonctionnel-technique'
        }
    )
    foreach ($documentInfo in $documents) {
        $baseName = $documentInfo.BaseName
        $sourcePath = $documentInfo.SourcePath
        $documentOutputDirectory = $documentInfo.OutputDirectory
        $null = New-Item -ItemType Directory -Path $documentOutputDirectory -Force
        $sourceHtml = [System.IO.File]::ReadAllText($sourcePath, [System.Text.Encoding]::UTF8)
        $diagramIndex = 0
        $figureCaptions = New-Object 'System.Collections.Generic.List[string]'
        $svgPattern = [regex]::new('<svg\b.*?</svg>', [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        $svgMatches = $svgPattern.Matches($sourceHtml)
        if ($svgMatches.Count -eq 0) {
            throw "No inline SVG diagrams were found in $sourcePath."
        }
        $builder = New-Object System.Text.StringBuilder
        $cursor = 0
        foreach ($match in $svgMatches) {
            [void]$builder.Append($sourceHtml.Substring($cursor, $match.Index - $cursor))
            $diagramIndex++
            $safeIndex = '{0:D2}' -f $diagramIndex
            $imagePath = Join-Path $imageDirectory "$baseName-Figure-$safeIndex.png"
            $bytes = [PortageSvgRasterizer]::Render($match.Value, 2)
            [System.IO.File]::WriteAllBytes($imagePath, $bytes)
            $labelMatch = [regex]::Match($match.Value, 'aria-label="([^"]+)"')
            $figureCaptions.Add($(if ($labelMatch.Success) { $labelMatch.Groups[1].Value } else { "Figure $diagramIndex" }))
            [void]$builder.Append("[[FIGURE:$safeIndex]]")
            $cursor = $match.Index + $match.Length
        }
        [void]$builder.Append($sourceHtml.Substring($cursor))
        $wordHtml = Convert-TableHtmlToParagraphs $builder.ToString()
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<head\b.*?</head>', '')
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<h1\b[^>]*>(.*?)</h1>', '[[H1]]$1[[/H1]]')
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<h2\b[^>]*>(.*?)</h2>', '[[H2]]$1[[/H2]]')
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<h3\b[^>]*>(.*?)</h3>', '[[H3]]$1[[/H3]]')
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<li\b[^>]*>', '- ')
        $wordHtml = [regex]::Replace($wordHtml, '(?is)</li\s*>', "`r")
        $wordHtml = [regex]::Replace($wordHtml, '(?is)<br\s*/?>', "`r")
        $wordHtml = [regex]::Replace($wordHtml, '(?is)</(p|section|blockquote|div|ol|ul|table)\s*>', "`r")
        $wordHtml = [regex]::Replace($wordHtml, '<[^>]+>', ' ')
        $wordText = [System.Net.WebUtility]::HtmlDecode($wordHtml)
        $paragraphs = New-Object 'System.Collections.Generic.List[object]'
        foreach ($line in ($wordText -split "`r|`n")) {
            $normalizedLine = ($line -replace '[ \t]+', ' ').Trim()
            if ($normalizedLine.Length -gt 0 -and $normalizedLine -notmatch '^\[\[FIGURE:\d+\]\]$') {
                $style = $null
                foreach ($headingLevel in 1..3) {
                    $headingMatch = [regex]::Match($normalizedLine, "^\[\[H$headingLevel\]\](.*?)\[\[/H$headingLevel\]\]$")
                    if ($headingMatch.Success) {
                        $normalizedLine = $headingMatch.Groups[1].Value
                        $style = "Heading$headingLevel"
                        break
                    }
                }
                $paragraphs.Add(@{ Text = $normalizedLine; Style = $style })
            }
        }
        $docxPath = Join-Path $documentOutputDirectory "$baseName.docx"
        $pdfPath = Join-Path $documentOutputDirectory "$baseName.pdf"

        Set-ConversionStatus "${baseName}: building editable Word package"
        New-OpenXmlDocument -DocumentPath $docxPath -BaseName $baseName -ImageDirectory $imageDirectory `
            -Paragraphs ($paragraphs.ToArray()) -FigureCaptions ($figureCaptions.ToArray())
        $edgePath = @(
            'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
            'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
        ) | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
        if (-not $edgePath) {
            throw 'Microsoft Edge is required to export the architecture HTML to PDF.'
        }
        Set-ConversionStatus "${baseName}: rendering PDF with Edge"
        if (Test-Path -LiteralPath $pdfPath) {
            Remove-Item -LiteralPath $pdfPath -Force
        }
        $profilePath = Join-Path $env:TEMP ("PortageEdge-{0}" -f [Guid]::NewGuid().ToString('N'))
        $sourceUri = ([Uri]$sourcePath).AbsoluteUri
        & $edgePath --headless --disable-gpu --no-first-run --no-default-browser-check `
            "--user-data-dir=$profilePath" "--print-to-pdf=$pdfPath" --no-pdf-header-footer $sourceUri | Out-Null
        $pdfReady = $false
        $previousLength = -1
        $stableChecks = 0
        $deadline = (Get-Date).AddSeconds(90)
        while ((Get-Date) -lt $deadline -and -not $pdfReady) {
            if (Test-Path -LiteralPath $pdfPath -PathType Leaf) {
                $pdfFile = Get-Item -LiteralPath $pdfPath
                if ($pdfFile.Length -gt 1000 -and $pdfFile.Length -eq $previousLength) {
                    $stableChecks++
                    if ($stableChecks -ge 2) {
                        $pdfReady = $true
                    }
                } else {
                    $stableChecks = 0
                    $previousLength = $pdfFile.Length
                }
            }
            if (-not $pdfReady) {
                Start-Sleep -Seconds 2
            }
        }
        if (-not $pdfReady) {
            throw "Microsoft Edge did not finish writing the PDF: $pdfPath"
        }
        $profileProcess = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$profilePath*" }
        foreach ($profileEntry in $profileProcess) {
            Stop-Process -Id $profileEntry.ProcessId -Force -ErrorAction SilentlyContinue
        }
        if (Test-Path -LiteralPath $profilePath) {
            Remove-Item -LiteralPath $profilePath -Recurse -Force
        }
        $profilePath = $null
        Set-ConversionStatus "${baseName}: verifying files"

        foreach ($deliverable in @($docxPath, $pdfPath)) {
            if (-not (Test-Path -LiteralPath $deliverable -PathType Leaf) -or (Get-Item -LiteralPath $deliverable).Length -lt 1000) {
                throw "The generated document is missing or unexpectedly small: $deliverable"
            }
        }
        Write-Output "$baseName`: $diagramIndex embedded figures; PDF exported."
    }
}
finally {
    if ($profilePath -and (Test-Path -LiteralPath $profilePath)) {
        $profileProcess = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$profilePath*" }
        foreach ($profileEntry in $profileProcess) {
            Stop-Process -Id $profileEntry.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Remove-Item -LiteralPath $profilePath -Recurse -Force
    }
}
