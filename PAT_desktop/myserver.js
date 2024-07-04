const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql');
const multer = require('multer');
const app = express();
app.use(bodyParser.json());


const conn = mysql.createConnection({
    host: "localhost",
    user: "root",
    password: "",
    database: "pat"
});

conn.connect(function(err) {
    if (err) throw err;
    console.log("Connected to MySQL ....");
});

// Set up multer storage and file handling
const storage = multer.diskStorage({
    destination: function(req, file, cb) {
        cb(null, './uploads'); // Specify the directory for storing uploads
    },
    filename: function(req, file, cb) {
        cb(null, file.originalname); // Use the original file name
    }
});

const fileFilter = (req, file, cb) => {
    const allowedTypes = ['text/plain'];
    if (allowedTypes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error('Only .txt files are allowed!'), false);
    }
};

const upload = multer({ 
    storage: storage,
    fileFilter: fileFilter
});

const path = require('path');
const fs = require('fs');

// Upload document and store path
app.post('/api/uploadDocument', upload.single('document'), function(req, res) {
    if (!req.file) {
        return res.status(400).send({
            status: 400,
            error: 'Only .txt files are allowed!',
            response: null
        });
    }

    const { owner_id, title } = req.body; // Changed from owner to owner_id
    const originalFileName = req.file.originalname;
    const docExtension = path.extname(originalFileName); // Extract file extension
    const newFileName = title + docExtension; // Construct new file name
    const docPath = req.file.path;
    const newDocPath = path.join(path.dirname(docPath), newFileName); // Construct new path with new file name

    // Rename the uploaded file
    fs.rename(docPath, newDocPath, function(err) {
        if (err) {
            console.error('Error renaming file:', err);
            return res.status(500).send({
                status: 500,
                error: 'Failed to rename uploaded file',
                response: null
            });
        }

        // Update the database with the new file name
        const sql = "INSERT INTO documents (owner_id, title, doc_path) VALUES (?, ?, ?)"; // Changed from owner to owner_id
        conn.query(sql, [owner_id, title, newDocPath], function(err, result) {
            if (err) {
                console.error('Error inserting into database:', err);
                return res.status(500).send({
                    status: 500,
                    error: 'Failed to save document information to database',
                    response: null
                });
            }

            res.send({
                status: 200,
                error: null,
                response: "Document uploaded and path saved successfully"
            });
        });
    });
});

// Edit document path and content
app.put('/api/editDocument/:doc_id', function(req, res) {
    const { doc_id } = req.params;
    const { title, content, owner_id } = req.body; // Changed from owner to owner_id

    const sqlCheckOwner = "SELECT * FROM documents WHERE doc_id = ? AND owner_id = ?"; // Changed from owner to owner_id
    const sqlUpdateDocument = "UPDATE documents SET title = ?, doc_path = ? WHERE doc_id = ?";

    // Check if the requester is the owner
    conn.query(sqlCheckOwner, [doc_id, owner_id], function(err, resultOwner) {
        if (err) throw err;

        if (resultOwner.length > 0) {
            const oldDocPath = resultOwner[0].doc_path;
            const newDocPath = path.join(path.dirname(oldDocPath), title + path.extname(oldDocPath));

            // Write new content to the file
            fs.writeFile(newDocPath, content, 'utf8', function(err) {
                if (err) {
                    console.error('Failed to write document content:', err);
                    return res.status(500).send({
                        status: 500,
                        error: "Failed to write document content",
                        response: null
                    });
                }

                // Update document path and title in the database
                conn.query(sqlUpdateDocument, [title, newDocPath, doc_id], function(err, result) {
                    if (err) {
                        console.error('Failed to update document in database:', err);
                        return res.status(500).send({
                            status: 500,
                            error: "Failed to update document in database",
                            response: null
                        });
                    }

                    res.send({
                        status: 200,
                        error: null,
                        response: "Document updated successfully"
                    });
                });
            });
        } else {
            res.status(403).send({
                status: 403,
                error: "Only the owner can update the document",
                response: null
            });
        }
    });
});

// Get document path by ID
app.get('/api/getDocumentPath/:doc_id', function(req, res) {
    const { doc_id } = req.params;
    const sql = "SELECT doc_path FROM documents WHERE doc_id = ?";

    conn.query(sql, [doc_id], function(err, result) {
        if (err) throw err;
        if (result.length > 0) {
            res.send({
                status: 200,
                error: null,
                response: result[0].doc_path
            });
        } else {
            res.status(404).send({
                status: 404,
                error: "Document not found",
                response: null
            });
        }
    });
});

// Register new user
const bcrypt = require('bcrypt');
const saltRounds = 10;

app.post('/api/registerUser', function(req, res) {
    const { username, password } = req.body;
    const sqlCheck = "SELECT * FROM users WHERE username = ?";

    conn.query(sqlCheck, [username], function(err, result) {
        if (err) throw err;
        if (result.length > 0) {
            res.status(400).send({
                status: 400,
                error: "Username already exists",
                response: null
            });
        } else {
            bcrypt.hash(password, saltRounds, function(err, hash) {
                if (err) throw err;
                const sql = "INSERT INTO users (username, password) VALUES (?, ?)";
                conn.query(sql, [username, hash], function(err, result) {
                    if (err) throw err;
                    res.send({
                        status: 200,
                        error: null,
                        response: {
                            message: "User registered successfully",
                            userId: result.insertId
                        }
                    });
                });
            });
        }
    });
});


// Login
app.post('/api/login', function(req, res) {
    const { username, password } = req.body;
    const sql = "SELECT * FROM users WHERE username = ?";
    const sqlUpdateActive = "UPDATE users SET is_active = true WHERE username = ?";

    conn.query(sql, [username], function(err, result) {
        if (err) throw err;
        if (result.length > 0) {
            const user = result[0];
            bcrypt.compare(password, user.password, function(err, isMatch) {
                if (err) throw err;
                if (isMatch) {
                    if (user.banned) {
                        res.status(403).send({
                            status: 403,
                            error: "Account is banned",
                            response: {
                                userId: user.id
                            }
                        });
                    } else if (!user.is_active) {
                        conn.query(sqlUpdateActive, [username], function(err, updateResult) {
                            if (err) throw err;
                            res.send({
                                status: 200,
                                error: null,
                                response: {
                                    message: "Login successful",
                                    userId: user.id
                                }
                            });
                        });
                    } else {
                        res.status(400).send({
                            status: 400,
                            error: "User is already logged in",
                            response: {
                                userId: user.id
                            }
                        });
                    }
                } else {
                    res.status(400).send({
                        status: 400,
                        error: "Invalid username or password",
                        response: {
                            userId: user.id
                        }
                    });
                }
            });
        } else {
            const sqlGetUserId = "SELECT id FROM users WHERE username = ?";
            conn.query(sqlGetUserId, [username], function(err, userResult) {
                if (err) throw err;
                if (userResult.length > 0) {
                    const userId = userResult[0].id;
                    res.status(400).send({
                        status: 400,
                        error: "Invalid username or password",
                        response: {
                            userId: userId
                        }
                    });
                } else {
                    res.status(400).send({
                        status: 400,
                        error: "Invalid username or password",
                        response: null
                    });
                }
            });
        }
    });
});

// Logout
app.post('/api/logout', function(req, res) {
    const { userId } = req.body;
    const sqlCheckActive = "SELECT is_active FROM users WHERE id = ?";
    const sqlUpdateActive = "UPDATE users SET is_active = false WHERE id = ?";

    conn.query(sqlCheckActive, [userId], function(err, result) {
        if (err) throw err;
        if (result.length > 0) {
            if (result[0].is_active) {
                conn.query(sqlUpdateActive, [userId], function(err, result) {
                    if (err) throw err;
                    res.send({
                        status: 200,
                        error: null,
                        response: "User logged out successfully"
                    });
                });
            } else {
                res.send({
                    status: 400,
                    error: "User is already logged out",
                    response: null
                });
            }
        } else {
            res.send({
                status: 404,
                error: "User not found",
                response: null
            });
        }
    });
});

// Delete document from documents table and filesystem
app.delete('/api/deleteDocument/:doc_id', function(req, res) {
    const { doc_id } = req.params;

    const sqlCheckOwner = "SELECT * FROM documents WHERE doc_id = ? AND owner_id = ?";
    const sqlDeleteDocument = "DELETE FROM documents WHERE doc_id = ?";

    // Check if the requester is the owner
    conn.query(sqlCheckOwner, [doc_id, req.body.owner_id], function(err, resultOwner) {
        if (err) throw err;

        if (resultOwner.length > 0) {
            const docPath = resultOwner[0].doc_path;

            // Owner is deleting the document
            conn.query(sqlDeleteDocument, [doc_id], function(err, result) {
                if (err) throw err;
                if (result.affectedRows > 0) {
                    // Delete the file from the filesystem
                    fs.unlink(docPath, (err) => {
                        if (err) {
                            console.error('Failed to delete file:', err);
                            res.status(500).send({
                                status: 500,
                                error: "Failed to delete the document file",
                                response: null
                            });
                        } else {
                            res.send({
                                status: 200,
                                error: null,
                                response: "Document deleted successfully"
                            });
                        }
                    });
                } else {
                    res.status(404).send({
                        status: 404,
                        error: "Document not found",
                        response: null
                    });
                }
            });
        } else {
            // Reject request if not owner
            res.status(403).send({
                status: 403,
                error: "Only the owner can delete the document",
                response: null
            });
        }
    });
});

// Get all documents by owner ID
app.get('/api/documentsByOwner/:owner_id', function(req, res) {
    const { owner_id } = req.params;
    const sql = "SELECT doc_id, title FROM documents WHERE owner_id = ?";

    conn.query(sql, [owner_id], function(err, result) {
        if (err) {
            res.status(500).send({
                status: 500,
                error: "Database error",
                response: null
            });
            return;
        } else {
            res.status(200).json(result);
        } 
    });
});


// Get document content by ID
app.get('/api/getDocumentContent/:doc_id', function(req, res) {
    const { doc_id } = req.params;
    console.log(`Fetching document content for doc_id: ${doc_id}`); // Debug log

    const sql = "SELECT doc_path FROM documents WHERE doc_id = ?";
    conn.query(sql, [doc_id], function(err, result) {
        if (err) {
            console.error("Database query error:", err); // Debug log
            return res.status(500).send({
                status: 500,
                error: "Failed to fetch document path",
                response: null
            });
        }
        
        if (result.length > 0) {
            const docPath = result[0].doc_path;
            console.log(`Document path found: ${docPath}`); // Debug log

            // Check if file exists before attempting to read
            fs.access(docPath, fs.constants.F_OK, (err) => {
                if (err) {
                    console.error("Document file does not exist:", err); // Debug log
                    return res.status(404).send({
                        status: 404,
                        error: "Document file not found",
                        response: null
                    });
                } else {
                    fs.readFile(docPath, 'utf8', (err, data) => {
                        if (err) {
                            console.error("Failed to read the document file:", err); // Debug log
                            return res.status(500).send({
                                status: 500,
                                error: "Failed to read the document file",
                                response: null
                            });
                        } else {
                            res.send({
                                status: 200,
                                error: null,
                                response: data
                            });
                        }
                    });
                }
            });
        } else {
            console.log("Document not found"); // Debug log
            res.status(404).send({
                status: 404,
                error: "Document not found",
                response: null
            });
        }
    });
});

// Ban user
app.post('/api/banUser', function(req, res) {
    const { adminId, userId } = req.body;

    if (adminId !== 1) {
        return res.status(403).send({
            status: 403,
            error: "Only admin can ban users",
            response: null
        });
    }

    const sql = "UPDATE users SET banned = 1 WHERE id = ?";
    conn.query(sql, [userId], function(err, result) {
        if (err) {
            console.error("Error updating user:", err);
            return res.status(500).send({
                status: 500,
                error: "Failed to ban user",
                response: null
            });
        }

        res.send({
            status: 200,
            error: null,
            response: "User banned successfully"
        });
    });
});

// Unban user
app.post('/api/unbanUser', function(req, res) {
    const { adminId, userId } = req.body;

    if (adminId !== 1) {
        return res.status(403).send({
            status: 403,
            error: "Only admin can unban users",
            response: null
        });
    }

    const sql = "UPDATE users SET banned = 0 WHERE id = ?";
    conn.query(sql, [userId], function(err, result) {
        if (err) {
            console.error("Error updating user:", err);
            return res.status(500).send({
                status: 500,
                error: "Failed to unban user",
                response: null
            });
        }

        res.send({
            status: 200,
            error: null,
            response: "User unbanned successfully"
        });
    });
});

// Fetch all users
app.get('/api/allUsers', function (req, res) {
    const sql = "SELECT id, username, password, is_active, banned FROM users";

    conn.query(sql, function (err, result) {
        if (err) {
            res.status(500).send({
                status: 500,
                error: "Internal Server Error",
                response: null
            });
        } else {
            res.send(result);
        }
    });
});

// Add endpoint to edit user password
app.put('/api/editPassword', (req, res) => {
    const { userId, newPassword } = req.body;

    if (!userId || !newPassword) {
        return res.status(400).json({ error: 'Missing userId or newPassword' });
    }

    const query = 'UPDATE users SET password = ? WHERE id = ?';
    conn.query(query, [newPassword, userId], (err, result) => {
        if (err) {
            console.error('Error updating password:', err);
            return res.status(500).json({ error: 'Failed to update password' });
        }
        res.status(200).json({ message: 'Password updated successfully' });
    });
});

const server = app.listen(8000, '0.0.0.0', function() {
    console.log("API Server running at port 8000");
});
