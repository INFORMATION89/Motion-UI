import os
import unittest

import pr_hygiene as h

TEMPLATE = """<!-- note -->
## Task
<!-- a -->
## Summary
<!-- b -->
"""


class BodyTests(unittest.TestCase):
    def test_headings_extracted_ignoring_comments(self):
        self.assertEqual(h.required_headings(TEMPLATE), ["Task", "Summary"])

    def test_complete_body_passes(self):
        self.assertEqual(h.check_body("## Task\nS6\n## Summary\nDone", TEMPLATE), [])

    def test_missing_section(self):
        errors = h.check_body("## Task\nS6", TEMPLATE)
        self.assertEqual(len(errors), 1)
        self.assertIn("Summary", errors[0])

    def test_comment_only_section_is_empty(self):
        errors = h.check_body("## Task\nS6\n## Summary\n<!-- todo -->", TEMPLATE)
        self.assertEqual(len(errors), 1)
        self.assertIn("empty", errors[0])

    def test_repo_template_headings_found(self):
        path = os.path.join(os.path.dirname(__file__), "..", h.TEMPLATE)
        with open(path, encoding="utf-8") as f:
            self.assertIn("Tool and tier", h.required_headings(f.read()))


class SkipCiTests(unittest.TestCase):
    def test_marker_on_code_commit_fails(self):
        errors = h.check_skip_ci([("a" * 40, "fix [skip ci]", ["app/Foo.kt"])])
        self.assertEqual(len(errors), 1)

    def test_marker_on_docs_commit_passes(self):
        files = ["README.md", "docs/CI.md", "LICENSE"]
        self.assertEqual(h.check_skip_ci([("a" * 40, "docs [ci skip]", files)]), [])

    def test_skip_checks_trailer(self):
        msg = "x\n\nskip-checks: true"
        self.assertEqual(len(h.check_skip_ci([("b" * 40, msg, [".github/x.yml"])])), 1)

    def test_no_marker_passes(self):
        self.assertEqual(h.check_skip_ci([("c" * 40, "feat", ["a.kt"])]), [])


if __name__ == "__main__":
    unittest.main()
