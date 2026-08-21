from __future__ import annotations

import os
import tempfile
import unittest
from pathlib import Path

from audit_publication import audit_repository


class PublicationAuditTests(unittest.TestCase):
    def audit_tree(self, files: dict[str, bytes | str]):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for relative, content in files.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                if isinstance(content, bytes):
                    path.write_bytes(content)
                else:
                    path.write_text(content, encoding="utf-8")
            return audit_repository(root)

    def test_rejects_sensitive_and_generated_material(self):
        result = self.audit_tree({
            "TRANS" + "CRIPT.txt": "x",
            "exchange/image.tar.xz": b"x",
            "run/private-evaluation.jsonl": "x",
            "quoter/target/app.jar": b"x",
            "cache/__pycache__/x.pyc": b"x",
        })
        self.assertFalse(result.ok)
        self.assertEqual(5, len(result.findings))

    def test_rejects_local_paths_email_and_secret_shapes(self):
        text = (
            "C:" + "\\Users\\Example "
            + "secret" + "_token=abc "
            + "person" + chr(64) + "example.invalid"
        )
        result = self.audit_tree({"README.md": text})
        self.assertFalse(result.ok)
        self.assertGreaterEqual(
            {finding.rule for finding in result.findings},
            {"absolute_path", "email_address", "secret_assignment"},
        )

    def test_accepts_sanitized_text_and_java(self):
        result = self.audit_tree({
            "README.md": "Evidence-driven trading systems",
            "src/Main.java": "final class Main {}\n",
        })
        self.assertTrue(result.ok)
        self.assertEqual(2, result.files_scanned)

    def test_rejects_challenge_names_and_protected_dataset_terms(self):
        result = self.audit_tree({
            "TASK" + ".md": "public notes",
            "notes.md": "development " + "hold" + "out dataset",
        })
        self.assertFalse(result.ok)
        self.assertGreaterEqual(
            {finding.rule for finding in result.findings},
            {"forbidden_path", "protected_dataset_term"},
        )

    def test_rejects_upload_url_binary_and_large_file(self):
        result = self.audit_tree({
            "notes.md": "https://example.invalid/" + "upload/abc",
            "image.dat": b"\x00binary",
            "large.txt": b"x" * (1024 * 1024 + 1),
        })
        self.assertFalse(result.ok)
        self.assertGreaterEqual(
            {finding.rule for finding in result.findings},
            {"upload_url", "binary_file", "large_file"},
        )

    def test_rejects_key_material_suffixes(self):
        result = self.audit_tree({
            "config/client.pem": "synthetic certificate material",
            "config/signing.key": "synthetic signing material",
            "config/client.p12": "synthetic container material",
            "config/client.pfx": "synthetic container material",
        })
        self.assertFalse(result.ok)
        self.assertEqual(
            {"forbidden_suffix"},
            {finding.rule for finding in result.findings},
        )

    @unittest.skipUnless(hasattr(os, "symlink"), "symlinks unavailable")
    def test_rejects_symlink_without_following_it(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "target.txt"
            target.write_text("safe", encoding="utf-8")
            link = root / "link.txt"
            try:
                os.symlink(target, link)
            except OSError as error:
                self.skipTest(f"symlink unavailable: {error}")
            result = audit_repository(root)
        self.assertFalse(result.ok)
        self.assertIn("symlink", {finding.rule for finding in result.findings})


if __name__ == "__main__":
    unittest.main(verbosity=2)
