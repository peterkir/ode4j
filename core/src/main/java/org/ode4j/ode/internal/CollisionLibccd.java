/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 * Open Dynamics Engine 4J, Copyright (C) 2009-2014 Tilmann Z�schke      *
 * All rights reserved.  Email: ode4j@gmx.de   Web: www.ode4j.org        *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT.         *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT, ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT for more   *
 * details.                                                              *
 *                                                                       *
 *************************************************************************/
package org.ode4j.ode.internal;

import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.*;
import org.ode4j.ode.internal.cpp4j.java.RefDouble;
import org.ode4j.ode.internal.libccd.CCD.ccd_center_fn;
import org.ode4j.ode.internal.libccd.CCD.ccd_support_fn;
import org.ode4j.ode.internal.libccd.CCDVec3.ccd_vec3_t;
import org.ode4j.ode.internal.trimesh.IFaceAngleStorageView;

import static org.ode4j.ode.DTriMesh.*;
import static org.ode4j.ode.internal.Common.*;
import static org.ode4j.ode.internal.CommonEnums.*;
import static org.ode4j.ode.internal.DxCollisionUtil.dQuatTransform;
import static org.ode4j.ode.internal.DxTriDataBase.FAD_CONCAVE;
import static org.ode4j.ode.internal.libccd.CCD.*;
import static org.ode4j.ode.internal.libccd.CCDMPR.*;
import static org.ode4j.ode.internal.libccd.CCDQuat.*;
import static org.ode4j.ode.internal.libccd.CCDVec3.*;
import static org.ode4j.ode.internal.Rotation.dQFromAxisAndAngle;
import static org.ode4j.ode.internal.Rotation.dQMultiply0;

/**
 * Lib ccd.
 *
 * @author Tilmann Zaeschke
 */
public class CollisionLibccd {

    private static class ccd_obj_t {
        final ccd_vec3_t pos = new ccd_vec3_t();
        final ccd_quat_t rot = new ccd_quat_t(), rot_inv = new ccd_quat_t();
    }

    ;
    //typedef struct _ccd_obj_t ccd_obj_t;

    private static class ccd_box_t extends ccd_obj_t {
        //ccd_obj_t o;
        double[] dim = new double[3];
    }

    ;
    //typedef struct _ccd_box_t ccd_box_t;

    static class ccd_cap_t extends ccd_obj_t {
        //ccd_obj_t o;
        double radius;
        final ccd_vec3_t axis = new ccd_vec3_t();
        final ccd_vec3_t p1 = new ccd_vec3_t();
        final ccd_vec3_t p2 = new ccd_vec3_t();
    }

    ;
    //typedef struct _ccd_cap_t ccd_cap_t;

    static class ccd_cyl_t extends ccd_obj_t {
        //ccd_obj_t o;
        double radius;
        final ccd_vec3_t axis = new ccd_vec3_t();
        final ccd_vec3_t p1 = new ccd_vec3_t();
        final ccd_vec3_t p2 = new ccd_vec3_t();
    }

    ;
    //typedef struct _ccd_cyl_t ccd_cyl_t;

    static class ccd_sphere_t extends ccd_obj_t {
        //ccd_obj_t o;
        double radius;
    }

    ;
    //typedef struct _ccd_sphere_t ccd_sphere_t;

    private static class ccd_convex_t extends ccd_obj_t {
        //ccd_obj_t o;
        DxConvex convex;
    }

    ;
    //typedef struct _ccd_convex_t ccd_convex_t;


    private static class ccd_triangle_t extends ccd_obj_t {
        ccd_vec3_t[] vertices = new ccd_vec3_t[]{new ccd_vec3_t(), new ccd_vec3_t(), new ccd_vec3_t()};
    }

    ;
    //typedef struct _ccd_triangle_t ccd_triangle_t;

    /** Transforms geom to ccd struct */
    //    static void ccdGeomToObj(const dGeomID g, ccd_obj_t *);
    //    static void ccdGeomToBox(const dGeomID g, ccd_box_t *);
    //    static void ccdGeomToCap(const dGeomID g, ccd_cap_t *);
    //    static void ccdGeomToCyl(const dGeomID g, ccd_cyl_t *);
    //    static void ccdGeomToSphere(const dGeomID g, ccd_sphere_t *);
    //    static void ccdGeomToConvex(const dGeomID g, ccd_convex_t *);

    /** Support functions */
    //    static void ccdSupportBox(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);
    //    static void ccdSupportCap(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);
    //    static void ccdSupportCyl(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);
    //    static void ccdSupportSphere(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);
    //    static void ccdSupportConvex(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);

    /** Center function */
    //    static void ccdCenter(const void *obj, ccd_vec3_t *c);

    /**
     * General collide function
     */
    //    static int ccdCollide(dGeomID o1, dGeomID o2, int flags,
    //                          dContactGeom *contact, int skip,
    //                          void *obj1, ccd_support_fn supp1, ccd_center_fn cen1,
    //                          void *obj2, ccd_support_fn supp2, ccd_center_fn cen2);

    //	static int collideCylCyl(dxGeom *o1, dxGeom *o2, ccd_cyl_t* cyl1, ccd_cyl_t* cyl2, int flags, dContactGeom
	//	*contacts, int skip);
    //	static bool testAndPrepareDiscContactForAngle(dReal angle, dReal radius, dReal length, dReal lSum, ccd_cyl_t
	//	*priCyl, ccd_cyl_t *secCyl, ccd_vec3_t &p, dReal &out_depth);
    //	// Adds a contact between 2 cylinders
    //	static int addCylCylContact(dxGeom *o1, dxGeom *o2, ccd_vec3_t* axis, dContactGeom *contacts, ccd_vec3_t* p,
	//	dReal normaldir, dReal depth, int j, int flags, int skip);
    //
    //	static unsigned addTrianglePerturbedContacts(dxGeom *o1, dxGeom *o2, IFaceAngleStorageView *meshFaceAngleView,
    //    const int *indices, unsigned numIndices, int flags, dContactGeom *contacts, int skip,
    //												 ccd_convex_t *c1, ccd_triangle_t *c2, dVector3 *triangle,
	//												 dContactGeom *contact, unsigned contacCount);
    //	static bool correctTriangleContactNormal(ccd_triangle_t *t, dContactGeom *contact, IFaceAngleStorageView
	//	*meshFaceAngleView, const int *indices, unsigned numIndices);
    //	static unsigned addUniqueContact(dContactGeom *contacts, dContactGeom *c, unsigned contactcount, unsigned
	//	maxcontacts, int flags, int skip);
    //	static void setObjPosToTriangleCenter(ccd_triangle_t *t);
    //	static void ccdSupportTriangle(const void *obj, const ccd_vec3_t *_dir, ccd_vec3_t *v);
    static void ccdGeomToObj(final DGeom g, ccd_obj_t o) {
        DVector3C ode_pos;
        DQuaternionC ode_rot;

        ode_pos = g.getPosition();
        ode_rot = g.getQuaternion();

        ccdVec3Set(o.pos, ode_pos);
        ccdQuatSet(o.rot, ode_rot.get1(), ode_rot.get2(), ode_rot.get3(), ode_rot.get0());

        ccdQuatInvert2(o.rot_inv, o.rot);
    }

    static void ccdGeomToBox(final DxBox g, ccd_box_t box) {
        DVector3 dim = new DVector3();

        ccdGeomToObj(g, box);

        g.getLengths(dim);
        box.dim[0] = dim.get0() * 0.5;
        box.dim[1] = dim.get1() * 0.5;
        box.dim[2] = dim.get2() * 0.5;
    }

    static void ccdGeomToCap(final DxCapsule g, ccd_cap_t cap) {
        //RefDouble r, h;
        ccdGeomToObj(g, cap);

        //dGeomCapsuleGetParams(g, r, h);
        cap.radius = g.getRadius();
        ccdVec3Set(cap.axis, 0.0, 0.0, g.getLength() / 2);
        ccdQuatRotVec(cap.axis, cap.rot);
        ccdVec3Copy(cap.p1, cap.axis);
        ccdVec3Copy(cap.p2, cap.axis);
        ccdVec3Scale(cap.p2, -1.0);
        ccdVec3Add(cap.p1, cap.pos);
        ccdVec3Add(cap.p2, cap.pos);
    }

    static void ccdGeomToCyl(final DxCylinder g, ccd_cyl_t cyl) {
        //double r, h;
        ccdGeomToObj(g, cyl);

        //dGeomCylinderGetParams(g, &r, &h);
        cyl.radius = g.getRadius();
        ccdVec3Set(cyl.axis, 0.0, 0.0, g.getLength() / 2);
        ccdQuatRotVec(cyl.axis, cyl.rot);
        ccdVec3Copy(cyl.p1, cyl.axis);
        ccdVec3Copy(cyl.p2, cyl.axis);
        boolean cylAxisNormalizationResult = ccdVec3Normalize(cyl.axis);
        dUVERIFY(cylAxisNormalizationResult == false, "Invalid cylinder has been passed");
        ccdVec3Normalize(cyl.axis);
        ccdVec3Scale(cyl.p2, -1.0);
        ccdVec3Add(cyl.p1, cyl.pos);
        ccdVec3Add(cyl.p2, cyl.pos);
    }

    static void ccdGeomToSphere(final DxSphere g, ccd_sphere_t s) {
        ccdGeomToObj(g, s);
        s.radius = g.getRadius();
    }

    static void ccdGeomToConvex(final DxConvex g, ccd_convex_t c) {
        ccdGeomToObj(g, c);
        c.convex = g;
    }


    private static final ccd_support_fn ccdSupportBox = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_box_t o = (ccd_box_t) obj;
            final ccd_vec3_t dir = new ccd_vec3_t();

            ccdVec3Copy(dir, _dir);
            ccdQuatRotVec(dir, o.rot_inv);

            ccdVec3Set(v, ccdSign(ccdVec3X(dir)) * o.dim[0], ccdSign(ccdVec3Y(dir)) * o.dim[1],
					ccdSign(ccdVec3Z(dir)) * o.dim[2]);

            // transform support vertex
            ccdQuatRotVec(v, o.rot);
            ccdVec3Add(v, o.pos);
        }
    };

    static final ccd_support_fn ccdSupportCap = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_cap_t o = (ccd_cap_t) obj;
            ccdVec3Copy(v, _dir);
            ccdVec3Scale(v, o.radius);
            if (ccdVec3Dot(_dir, o.axis) > 0.0) {
                ccdVec3Add(v, o.p1);
            } else {
                ccdVec3Add(v, o.p2);
            }
        }
    };

    static final ccd_support_fn ccdSupportCyl = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_cyl_t cyl = (ccd_cyl_t) obj;
            final ccd_vec3_t dir = new ccd_vec3_t();

            double dot = ccdVec3Dot(_dir, cyl.axis);
            if (dot > 0.0) {
                ccdVec3Copy(v, cyl.p1);
            } else {
                ccdVec3Copy(v, cyl.p2);
            }
            // project dir onto cylinder 'top'/'bottom' plane
            ccdVec3Copy(dir, cyl.axis);
            ccdVec3Scale(dir, -dot);
            ccdVec3Add(dir, _dir);
            double len = CCD_SQRT(ccdVec3Len2(dir));
            if (!ccdIsZero(len)) {
                ccdVec3Scale(dir, cyl.radius / len);
                ccdVec3Add(v, dir);
            }
        }
    };

    static final ccd_support_fn ccdSupportSphere = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_sphere_t s = (ccd_sphere_t) obj;
            ccdVec3Copy(v, _dir);
            ccdVec3Scale(v, s.radius);
            if (!dNODEBUG)
                dIASSERT(Common.dFabs(CCD_SQRT(ccdVec3Len2(_dir)) - 1.0) < 1e-6); // ccdVec3Scale(v, CCD_ONE /
			// CCD_SQRT(ccdVec3Len2(_dir)));
            ccdVec3Add(v, s.pos);
        }
    };

    private static final ccd_support_fn ccdSupportConvex = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_convex_t c = (ccd_convex_t) obj;
            final ccd_vec3_t dir = new ccd_vec3_t(), p = new ccd_vec3_t();
            double maxdot, dot;
            int i;
            double[] curp;

            ccdVec3Copy(dir, _dir);
            ccdQuatRotVec(dir, c.rot_inv);

            maxdot = -CCD_REAL_MAX;
            curp = c.convex.getPoints();
            int curpI = 0;
            for (i = 0; i < c.convex.getPointcount(); i++, curpI += 3) {
                ccdVec3Set(p, curp[curpI + 0], curp[curpI + 1], curp[curpI + 2]);
                dot = ccdVec3Dot(dir, p);
                if (dot > maxdot) {
                    ccdVec3Copy(v, p);
                    maxdot = dot;
                }
            }


            // transform support vertex
            ccdQuatRotVec(v, c.rot);
            ccdVec3Add(v, c.pos);
        }
    };

    private static final ccd_center_fn ccdCenter = new ccd_center_fn() {
        @Override
        public void run(Object obj1, ccd_vec3_t c) {
            final ccd_obj_t o = (ccd_obj_t) obj1;
            ccdVec3Copy(c, o.pos);
        }
    };


    static int ccdCollide(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts, ccd_obj_t obj1,
						  ccd_support_fn supp1, ccd_center_fn cen1, ccd_obj_t obj2, ccd_support_fn supp2,
						  ccd_center_fn cen2) {
        ccd_t ccd = new ccd_t();
        int res;
        final RefDouble depth = new RefDouble();
        final ccd_vec3_t dir = new ccd_vec3_t(), pos = new ccd_vec3_t();
        int max_contacts = (flags & DxGeom.NUMC_MASK);

        if (max_contacts < 1) return 0;

        CCD_INIT(ccd);
        ccd.support1 = supp1;
        ccd.support2 = supp2;
        ccd.center1 = cen1;
        ccd.center2 = cen2;
        ccd.max_iterations = 500;
        ccd.mpr_tolerance = 1E-6;


        if ((flags & OdeConstants.CONTACTS_UNIMPORTANT) != 0) {
            if (ccdMPRIntersect(obj1, obj2, ccd) != 0) {
                return 1;
            } else {
                return 0;
            }
        }

        res = ccdMPRPenetration(obj1, obj2, ccd, depth, dir, pos);
        if (res == 0) {
            DContactGeom contact = contacts.get();
            contact.g1 = o1;
            contact.g2 = o2;

            contact.side1 = contact.side2 = -1;

            contact.depth = depth.get();

            contact.pos.set(ccdVec3X(pos), ccdVec3Y(pos), ccdVec3Z(pos));

            ccdVec3Scale(dir, -1.);
            contact.normal.set(ccdVec3X(dir), ccdVec3Y(dir), ccdVec3Z(dir));

            return 1;
        }

        return 0;
    }

    public static class CollideBoxCylinderCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_cyl_t cyl = new ccd_cyl_t();
            ccd_box_t box = new ccd_box_t();
            ccdGeomToBox((DxBox) o1, box);
            ccdGeomToCyl((DxCylinder) o2, cyl);

            return ccdCollide(o1, o2, flags, contacts, box, ccdSupportBox, ccdCenter, cyl, ccdSupportCyl, ccdCenter);
        }
    }

    ;

    public static class CollideCapsuleCylinder implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_cap_t cap = new ccd_cap_t();
            ccd_cyl_t cyl = new ccd_cyl_t();

            ccdGeomToCap((DxCapsule) o1, cap);
            ccdGeomToCyl((DxCylinder) o2, cyl);

            return ccdCollide(o1, o2, flags, contacts, cap, ccdSupportCap, ccdCenter, cyl, ccdSupportCyl, ccdCenter);
        }
    }

    ;

    public static class CollideConvexBoxCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_box_t box = new ccd_box_t();
            ccd_convex_t conv = new ccd_convex_t();

            ccdGeomToConvex((DxConvex) o1, conv);
            ccdGeomToBox((DxBox) o2, box);

            return ccdCollide(o1, o2, flags, contacts, conv, ccdSupportConvex, ccdCenter, box, ccdSupportBox,
					ccdCenter);
        }
    }

    ;

    public static class CollideConvexCapsuleCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_cap_t cap = new ccd_cap_t();
            ccd_convex_t conv = new ccd_convex_t();

            ccdGeomToConvex((DxConvex) o1, conv);
            ccdGeomToCap((DxCapsule) o2, cap);

            return ccdCollide(o1, o2, flags, contacts, conv, ccdSupportConvex, ccdCenter, cap, ccdSupportCap,
					ccdCenter);
        }
    }

    ;

    public static class CollideConvexSphereCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_sphere_t sphere = new ccd_sphere_t();
            ccd_convex_t conv = new ccd_convex_t();

            ccdGeomToConvex((DxConvex) o1, conv);
            ccdGeomToSphere((DxSphere) o2, sphere);

            return ccdCollide(o1, o2, flags, contacts, conv, ccdSupportConvex, ccdCenter, sphere, ccdSupportSphere,
					ccdCenter);
        }
    }

    ;

    public static class CollideConvexCylinderCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_cyl_t cyl = new ccd_cyl_t();
            ccd_convex_t conv = new ccd_convex_t();

            ccdGeomToConvex((DxConvex) o1, conv);
            ccdGeomToCyl((DxCylinder) o2, cyl);

            return ccdCollide(o1, o2, flags, contacts, conv, ccdSupportConvex, ccdCenter, cyl, ccdSupportCyl,
					ccdCenter);
        }
    }

    ;

    public static class CollideConvexConvexCCD implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            ccd_convex_t c1 = new ccd_convex_t(), c2 = new ccd_convex_t();

            ccdGeomToConvex((DxConvex) o1, c1);
            ccdGeomToConvex((DxConvex) o2, c2);

            return ccdCollide(o1, o2, flags, contacts, c1, ccdSupportConvex, ccdCenter, c2, ccdSupportConvex,
					ccdCenter);
        }
    }

    public static class CollideCylinderCylinder implements DColliderFn {
        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags, DContactGeomBuffer contacts) {
            final ccd_cyl_t cyl1 = new ccd_cyl_t(), cyl2 = new ccd_cyl_t();

            ccdGeomToCyl((DxCylinder) o1, cyl1);
            ccdGeomToCyl((DxCylinder) o2, cyl2);

            int numContacts = CollisionLibccdCylinderStacking.collideCylCyl(o1, o2, cyl1, cyl2, flags, contacts);
            if (numContacts < 0) {
                numContacts = ccdCollide(o1, o2, flags, contacts, cyl1, ccdSupportCyl, ccdCenter, cyl2, ccdSupportCyl
						, ccdCenter);
            }
            return numContacts;
        }
    }


    private static final float CONTACT_POS_EPSILON = 0.001f;
    private static final float CONTACT_PERTURBATION_ANGLE = 0.005f;

    public static class CollideConvexTrimeshTrianglesCCD {
        public int collide(DGeom o1, DGeom o2, int[] triindices, int flags, DContactGeomBuffer contacts) {
            ccd_convex_t c1 = new ccd_convex_t();
            ccd_triangle_t c2 = new ccd_triangle_t();
            final DVector3[] triangle = new DVector3[]{new DVector3(), new DVector3(), new DVector3()};
            int maxContacts = (flags & DxGeom.NUMC_MASK);
            int contactCount = 0;
            ccdGeomToConvex((DxConvex) o1, c1);
            ccdGeomToObj(o2, c2);

            IFaceAngleStorageView meshFaceAngleView = dxGeomTriMeshGetFaceAngleView(o2);
            dUASSERT(meshFaceAngleView != null, "Please preprocess the trimesh data with " +
					"dTRIDATAPREPROCESS_BUILD_FACE_ANGLES");

            DContactGeomBuffer tempContacts = new DContactGeomBuffer(1);
            for (int i : triindices) {
                ((DxTriMesh) o2).FetchTransformedTriangle(i, triangle);
                for (int j = 0; j < 3; j++) {
                    c2.vertices[j].set(triangle[j].get0(), triangle[j].get1(), triangle[j].get2());
                }
                setObjPosToTriangleCenter(c2);
                if (ccdCollide(o1, o2, flags, tempContacts, c1, ccdSupportConvex, ccdCenter, c2, ccdSupportTriangle,
						ccdCenter) == 1) {
                    DContactGeom tempContact = tempContacts.get();
                    tempContact.side2 = i;
                    if (meshFaceAngleView == null && correctContactNormal(c2, tempContact, triangle, o2, triindices)) {
                        contactCount = addUniqueContact(contacts, tempContact, contactCount, maxContacts);
                        if ((flags & OdeConstants.CONTACTS_UNIMPORTANT) != 0) {
                            break;
                        }
                    }
                }
            }
            if (contactCount == 1 && (flags & OdeConstants.CONTACTS_UNIMPORTANT) == 0) {
                DContactGeom contact = contacts.getSafe(flags, 0);
                ((DxTriMesh) o2).FetchTransformedTriangle(contact.side2, triangle);
                contactCount = addPerturbedContacts(o1, o2, flags, c1, c2, contacts, triangle, contact, contactCount,
						triindices);
                // Normalize accumulated normals
                //				for (int i = 0; i < contactcount; i++) {
                //					contact = contacts.get(i);
                //					contact.normal.safeNormalize();
                //				}
            }
            // Normalize accumulated normals, if necessary
            for (int k = 0; k != contactCount; ) {
                DContactGeom contact = contacts.getSafe(flags, k);
                boolean stayWithinThisIndex = false;

                // Only the merged contact normals need to be normalized
                if (_const_type_cast_union < bool > (contact.normal[dV3E_PAD])) {

                    if (!contact.normal.safeNormalize()) {
                        // If the contact normals have added up to zero, erase the contact
                        // Normally the time step is to be shorter so that the objects do not get into each other
						// that deep
                        --contactCount;

                        if (k != contactCount) {
                            DContactGeom lastContact = contacts.getSafe(flags, contactCount);
                            contact = lastContact;
                        }

                        stayWithinThisIndex = true;
                    }
                }

                if (!stayWithinThisIndex) {
                    ++k;
                }
            }

            return contactCount;
        }

        private static int addTrianglePerturbedContacts(DxGeom o1, DxGeom o2, IFaceAngleStorageView meshFaceAngleView
				, final int[] indices, int numIndices, int flags, DContactGeomBuffer contacts, int skip,
														ccd_convex_t c1, ccd_triangle_t c2, DVector3[] triangle,
														DContactGeom contact, int contacCount) {
            int maxContacts = (flags & DxGeom.NUMC_MASK);

            DVector3 pos = new DVector3(contact.pos);

            final DQuaternion[] q1 = new DQuaternion[]{new DQuaternion(), new DQuaternion()};
            final DQuaternion[] q2 = new DQuaternion[]{new DQuaternion(), new DQuaternion()};
            double perturbationAngle = CONTACT_PERTURBATION_ANGLE;

            DVector3 upAxis = new DVector3();
            boolean upAvailable = false;
            if (Math.abs(contact.normal.get(dV3E_Y)) > 0.7) {
                upAxis.set(0, 0, 1);
            } else {
                upAxis.set(0, 1, 0);
            }

            DVector3 cross = new DVector3();
            OdeMath.dCalcVectorCross3(cross, contact.normal, upAxis);

            if (cross.safeNormalize()) {
                OdeMath.dCalcVectorCross3(upAxis, cross, contact.normal);

                if (upAxis.safeNormalize()) {
                    upAvailable = true;
                }
            }

            for (int j = upAvailable ? 0 : 2; j != 2; ++j) {
                //				dQFromAxisAndAngle(q1[j], upAxis[dV3E_X], upAxis[dV3E_Y], upAxis[dV3E_Z],
				//				perturbationAngle);
                //				dQFromAxisAndAngle(q2[j], cross[dV3E_X], cross[dV3E_Y], cross[dV3E_Z],
				//				perturbationAngle);
                dAssertVec3Element();
                dQFromAxisAndAngle(q1[j], upAxis, perturbationAngle);
                dQFromAxisAndAngle(q2[j], cross, perturbationAngle);
                perturbationAngle = -perturbationAngle;
            }

            for (int k = upAvailable ? 0 : 4; k != 4; ++k) {
                DQuaternion qr = new DQuaternion();
                dQMultiply0(qr, q1[k % 2], q2[k / 2]);

                for (int j = dMTV__MIN; j != dMTV__MAX; ++j) {
                    DVector3 p = new DVector3();
                    DVector3 perturbed = new DVector3();
                    p.eqDiff(triangle[j], pos); //dSubtractVectors3(p, triangle[j], pos);
                    dQuatTransform(qr, p, perturbed);
                    perturbed.add(pos); //dAddVectors3(perturbed, perturbed, pos);

                    dAssertVec3Element();
                    // ccdVec3Set(c2.vertices[j], perturbed[dV3E_X], perturbed[dV3E_Y], perturbed[dV3E_Z]);
                    ccdVec3Set(c2.vertices[j], perturbed);
                }

                DContactGeom perturbedContact = new DContactGeom();
                setObjPosToTriangleCenter(c2);

                if (ccdCollide(o1, o2, flags, perturbedContact, c1, ccdSupportConvex, ccdCenter, c2,
						ccdSupportTriangle, ccdCenter) == 1) {
                    perturbedContact.side2 = contact.side2;

                    if (meshFaceAngleView == null || correctTriangleContactNormal(c2, perturbedContact,
							meshFaceAngleView, indices, numIndices)) {
                        contacCount = addUniqueContact(contacts, perturbedContact, contacCount, maxContacts, flags,
								skip);
                    }
                }
            }

            return contacCount;
        }

        static boolean correctTriangleContactNormal(ccd_triangle_t t, DContactGeom contact,
                                                    IFaceAngleStorageView meshFaceAngleView, final int[] indices,
                                                    int numIndices) {
            dIASSERT(meshFaceAngleView != null);

            boolean anyFault = false;

            ccd_vec3_t cntOrigNormal = new ccd_vec3_t(), cntNormal = new ccd_vec3_t();
            ccdVec3Set(cntNormal, contact.normal);
            ccdVec3Copy(cntOrigNormal, cntNormal);

            // Check if the contact point is located close to any edge - move it back and forth
            // and check the resulting segment for intersection with the edge plane
            ccd_vec3_t cntScaledNormal = new ccd_vec3_t();
            ccdVec3CopyScaled(cntScaledNormal, cntNormal, contact.depth);

            ccd_vec3_t[] edges = new ccd_vec3_t[]{new ccd_vec3_t(), new ccd_vec3_t()};
            assert (dMTV__MAX == 2);
            ccdVec3Sub2(edges[dMTV_THIRD], t.vertices[0], t.vertices[2]);
            ccdVec3Sub2(edges[dMTV_SECOND], t.vertices[2], t.vertices[1]);
            ccdVec3Sub2(edges[dMTV_FIRST], t.vertices[1], t.vertices[0]);
            dAASSERT(dMTV__MAX == 3);

            boolean contactGenerated = false;
            boolean contactPreserved = false;
            // Triangle face normal
            ccd_vec3_t triNormal = new ccd_vec3_t();
            ccdVec3Cross(triNormal, edges[dMTV_FIRST], edges[dMTV_SECOND]);
            if (ccdVec3Normalize(triNormal)) {
                anyFault = true;
            }

            // Check the edges to see if one of them is involved
            for (int testEdgeIndex = !anyFault ? dMTV__MIN : dMTV__MAX; testEdgeIndex != dMTV__MAX; ++testEdgeIndex) {
                ccd_vec3_t edgeNormal = new ccd_vec3_t(), vertexToPos = new ccd_vec3_t(), v = new ccd_vec3_t();
                ccd_vec3_t edgeAxis = edges[testEdgeIndex];

                // Edge axis
                if (ccdVec3Normalize(edgeAxis)) {
                    // This should not happen normally as in the case on of edges is degenerated
                    // the triangle normal calculation would have to fail above. If for some
                    // reason the above calculation succeeds and this one would not, it is
                    // OK to break as this point as well.
                    anyFault = true;
                    break;
                }

                // Edge Normal
                ccdVec3Cross(edgeNormal, edgeAxis, triNormal);
                // ccdVec3Normalize(&edgeNormal); -- the two vectors above were already normalized and perpendicular

                // Check if the contact point is located close to any edge - move it back and forth
                // and check the resulting segment for intersection with the edge plane
                ccdVec3Set(vertexToPos, contact.pos);
                ccdVec3Sub(vertexToPos, t.vertices[testEdgeIndex]);
                ccdVec3Sub2(v, vertexToPos, cntScaledNormal);

                if (ccdVec3Dot(edgeNormal, v) < 0) {
                    ccdVec3Add2(v, vertexToPos, cntScaledNormal);

                    if (ccdVec3Dot(edgeNormal, v) > 0) {
                        // This is an edge contact

                        double x = ccdVec3Dot(triNormal, cntNormal);
                        double y = ccdVec3Dot(edgeNormal, cntNormal);
                        double contactNormalToTriangleNormalAngle = CCD_ATAN2(y, x);

                        RefDouble angleValueAsDRead = new RefDouble();
                        //FaceAngleDomain
                        int angleDomain = meshFaceAngleView.retrieveFacesAngleFromStorage(angleValueAsDRead,
								contact.side2, /*(dMeshTriangleVertex)*/ testEdgeIndex);
                        double angleValue = angleValueAsDRead.get();

                        double targetAngle = 0;
                        contactGenerated = false;
                        contactPreserved = false; // re-assign to make optimizer's task easier

                        if (angleDomain != FAD_CONCAVE) {
                            // Convex or flat - ensure the contact normal is within the allowed range
                            // formed by the two triangles' normals.
                            if (contactNormalToTriangleNormalAngle < CCD_ZERO) {
                                targetAngle = CCD_ZERO;
                            } else if (contactNormalToTriangleNormalAngle > angleValue) {
                                targetAngle = angleValue;
                            } else {
                                contactPreserved = true;
                            }
                        } else {
                            // Concave - rotate the contact normal to the face angle bisect plane
                            // (or to triangle normal-edge plane if negative angles are not stored)
                            targetAngle = angleValue != 0 ? 0.5 * angleValue : CCD_ZERO;
                            // There is little chance the normal will initially match the correct plane, but still, a
							// small check could save lots of calculations
                            if (contactNormalToTriangleNormalAngle == targetAngle) {
                                contactPreserved = true;
                            }
                        }

                        if (!contactPreserved) {
                            ccd_quat_t q = new ccd_quat_t();
                            ccdQuatSetAngleAxis(q, targetAngle - contactNormalToTriangleNormalAngle, edgeAxis);
                            ccdQuatRotVec2(cntNormal, cntNormal, q);
                            contactGenerated = true;
                        }

                        // Calculated successfully
                        break;
                    }
                }
            }

            if (!anyFault && !contactPreserved) {
                // No edge contact detected, set contact normal to triangle normal
                final ccd_vec3_t cntNormalToUse = !contactGenerated ? triNormal : cntNormal;
                dAssertVec3Element();
                contact.normal.set(ccdVec3X(cntNormalToUse), ccdVec3Y(cntNormalToUse), ccdVec3Z(cntNormalToUse));
                contact.depth *= CCD_FMAX(0.0, ccdVec3Dot(cntOrigNormal, cntNormalToUse));
            }

            boolean result = !anyFault;
            return result;
        }


        static int addUniqueContact(DContactGeomBuffer contacts, DContactGeom c, int contactcount, int maxcontacts,
									int flags, int skip) {
            double minDepth = c.depth;
            int index = contactcount;
            boolean isDuplicate = false;

            dAssertVec3Element();
            //double c_posX = c.pos[dVec3Element.dV3E_X], c_posY = c.pos[dVec3Element.dV3E_Y], c_posZ = c
			// .pos[dVec3Element.dV3E_Z];
            double c_posX = c.pos.get0(), c_posY = c.pos.get1(), c_posZ = c.pos.get2();
            for (int k = 0; k != contactcount; k++) {
                DContactGeom pc = contacts.getSafe(flags, k);

                //				if (Math.abs(c_posX - pc.pos[dV3E_X]) < CONTACT_POS_EPSILON
                //						&& Math.abs(c_posY - pc.pos[dV3E_Y]) < CONTACT_POS_EPSILON
                //						&& Math.abs(c_posZ - pc.pos[dV3E_Z]) < CONTACT_POS_EPSILON) {
                //					dAASSERT(dV3E__AXES_MAX - dV3E__AXES_MIN == 3);
                dAssertVec3Element();
                if (Math.abs(c_posX - pc.pos.get0()) < CONTACT_POS_EPSILON && Math.abs(c_posY - pc.pos.get1()) < CONTACT_POS_EPSILON && Math.abs(c_posZ - pc.pos.get2()) < CONTACT_POS_EPSILON) {

                    // Accumulate similar contacts
                    pc.normal.add(c.normal); //dAddVectors3(pc.normal, pc.normal, c.normal);
                    pc.depth = dMax(pc.depth, c.depth);
                    if (true) throw new UnsupportedOperationException(); // TODO CHECK-TZ fix the following!
                    //               _type_cast_union<bool>(pc.normal[dV3E_PAD]) = true; // Mark the contact as a
					//               merged one

                    isDuplicate = true;
                    break;
                }

                if (contactcount == maxcontacts && pc.depth < minDepth) {
                    minDepth = pc.depth;
                    index = k;
                }
            }

            if (!isDuplicate && index < maxcontacts) {
                DContactGeom contact = contacts.getSafe(flags, index);
                contact.g1 = c.g1;
                contact.g2 = c.g2;
                contact.depth = c.depth;
                contact.side1 = c.side1;
                contact.side2 = c.side2;
                contact.pos.set(c.pos); //dCopyVector3(contact.pos, c.pos);
                contact.normal.set(c.normal);//dCopyVector3(contact.normal, c.normal);
                if (true) throw new UnsupportedOperationException(); // TODO CHECK-TZ fix the following!
                //        *_type_cast_union<bool>(contact.normal[dV3E_PAD]) = false; // Indicates whether the contact
				//        is merged or not
                contactcount = index == contactcount ? contactcount + 1 : contactcount;
            }

            return contactcount;
        }

        private static void setObjPosToTriangleCenter(ccd_triangle_t t) {
            ccdVec3Set(t.pos, 0, 0, 0);
            for (int j = 0; j < 3; j++) {
                ccdVec3Add(t.pos, t.vertices[j]);
            }
            ccdVec3Scale(t.pos, 1.0f / 3.0f);
        }

        private boolean correctContactNormal(ccd_triangle_t t, DContactGeom contact, DVector3[] triangle, DGeom geom,
											 int[] indices) {
            ccd_vec3_t edge1 = new ccd_vec3_t();
            ccd_vec3_t edge2 = new ccd_vec3_t();
            ccdVec3Copy(edge1, t.vertices[1]);
            ccdVec3Sub(edge1, t.vertices[0]);
            ccdVec3Copy(edge2, t.vertices[2]);
            ccdVec3Sub(edge2, t.vertices[1]);
            ccd_vec3_t triangleNormal = new ccd_vec3_t();
            ccdVec3Cross(triangleNormal, edge1, edge2);
            // Triangle face normal
            if (!ccdVec3Normalize(triangleNormal)) {
                return false;
            }
            ccd_vec3_t contactNormal = new ccd_vec3_t();
            ccdVec3Set(contactNormal, contact.normal);
            ccd_vec3_t scaledContactNormal = new ccd_vec3_t();
            ccdVec3Copy(scaledContactNormal, contactNormal);
            ccdVec3Scale(scaledContactNormal, contact.depth);
            boolean edgeContact = false;
            // Check the edges to see if one of them is involved
            for (int i = 0; i < 3; i++) {
                ccd_vec3_t edgeAxis = new ccd_vec3_t();
                ccdVec3Copy(edgeAxis, t.vertices[(i + 1) % 3]);
                ccdVec3Sub(edgeAxis, t.vertices[i]);
                // Edge axis
                if (!ccdVec3Normalize(edgeAxis)) {
                    return false;
                }
                // Edge Normal
                ccd_vec3_t edgeNormal = new ccd_vec3_t();
                ccdVec3Cross(edgeNormal, edgeAxis, triangleNormal);
                ccdVec3Normalize(edgeNormal);
                // Check if the contact point is located close to any edge -
                // move it
                // back and forth and check the resulting segment for
                // intersection
                // with the edge plane
                ccd_vec3_t v = new ccd_vec3_t();
                ccdVec3Set(v, contact.pos);
                ccdVec3Sub(v, t.vertices[i]);
                ccdVec3Sub(v, scaledContactNormal);
                if (ccdVec3Dot(edgeNormal, v) < 0) {
                    ccdVec3Set(v, contact.pos);
                    ccdVec3Sub(v, t.vertices[i]);
                    ccdVec3Add(v, scaledContactNormal);
                    if (ccdVec3Dot(edgeNormal, v) > 0) {
                        // Edge contact, check the edge angle now
                        double x = ccdVec3Dot(triangleNormal, contactNormal);
                        double y = ccdVec3Dot(edgeNormal, contactNormal);
                        double contactNormalToTriangleNormalAngle = Math.atan2(y, x);
                        double edgeAngle = ((DxTriMesh) geom).getEdgeAngle(contact.side2, i);
                        double targetAngle;
                        edgeContact = true;
                        if (edgeAngle > 0) {
                            // Convex edge - ensure the contact normal is within
                            // the allowed range formed by the two triangles' normals, if not rotate it
                            targetAngle = Math.min(contactNormalToTriangleNormalAngle, edgeAngle);
                        } else {
                            // Concave edge
                            targetAngle = 0.0;
                        }
                        double angle = targetAngle - contactNormalToTriangleNormalAngle;
                        if (angle != 0.0) {
                            ccd_quat_t q = new ccd_quat_t();
                            ccdQuatSetAngleAxis(q, angle, edgeAxis);
                            ccdQuatRotVec(contactNormal, q);
                        }
                        break;
                    }
                }
            }
            // If no edge contact detected, set contact normal to triangle normal
            ccd_vec3_t origContactNormal = new ccd_vec3_t();
            ccdVec3Set(origContactNormal, contact.normal);
            ccd_vec3_t contactNormalToUse = edgeContact ? contactNormal : triangleNormal;
            contact.normal.set(ccdVec3X(contactNormalToUse), ccdVec3Y(contactNormalToUse), ccdVec3Z(contactNormalToUse));
            // Reduce the depth as the normal changed
            contact.depth *= Math.max(0.0, ccdVec3Dot(origContactNormal, contactNormalToUse));
            return true;
        }

        private int addUniqueContact(DContactGeomBuffer contacts, DContactGeom c, int contactcount, int maxcontacts) {
            double minDepth = c.depth;
            int index = contactcount;
            boolean unique = true;
            for (int k = 0; k < contactcount; k++) {
                DContactGeom pc = contacts.get(k);
                if (Math.abs(c.pos.get0() - pc.pos.get0()) < CONTACT_POS_EPSILON && Math.abs(c.pos.get1() - pc.pos.get1()) < CONTACT_POS_EPSILON && Math.abs(c.pos.get2() - pc.pos.get2()) < CONTACT_POS_EPSILON) {
                    // Accumulate similar contacts
                    pc.normal.add(c.normal);
                    pc.depth = Math.max(pc.depth, c.depth);
                    unique = false;
                    break;
                }
                if (contactcount == maxcontacts && pc.depth < minDepth) {
                    minDepth = pc.depth;
                    index = k;
                }
            }
            if (unique && index < maxcontacts) {
                DContactGeom contact = contacts.get(index);
                contact.g1 = c.g1;
                contact.g2 = c.g2;
                contact.depth = c.depth;
                contact.side1 = c.side1;
                contact.side2 = c.side2;
                contact.pos.set(c.pos);
                contact.normal.set(c.normal);
                contactcount = index == contactcount ? contactcount + 1 : contactcount;
            }
            return contactcount;
        }

        private int addPerturbedContacts(DGeom o1, DGeom o2, int flags, ccd_convex_t c1, ccd_triangle_t c2, DContactGeomBuffer contacts, DVector3[] triangle, DContactGeom contact, int contactcount, int[] indices) {
            int maxcontacts = (flags & 0xffff);
            DVector3 upAxis = new DVector3(0, 1, 0);
            if (Math.abs(contact.normal.dot(upAxis)) > 0.7) {
                upAxis.set(0, 0, 1);
            }
            DVector3 cross = new DVector3();
            cross.eqCross(contact.normal, upAxis);
            cross.safeNormalize();
            upAxis.eqCross(cross, contact.normal);
            upAxis.safeNormalize();
            DVector3 perturbed = new DVector3();
            DVector3 pos = new DVector3(contact.pos);
            DContactGeomBuffer perturbedContacts = new DContactGeomBuffer(1);

            DQuaternion[] q1 = new DQuaternion[]{new DQuaternion(), new DQuaternion()};
            DQuaternion[] q2 = new DQuaternion[]{new DQuaternion(), new DQuaternion()};
            double perturbationAngle = CONTACT_PERTURBATION_ANGLE;
            for (int j = 0; j < 2; ++j) {
                dQFromAxisAndAngle(q1[j], upAxis, perturbationAngle);
                dQFromAxisAndAngle(q2[j], cross, perturbationAngle);
                perturbationAngle = -perturbationAngle;
            }
            DQuaternion qr = new DQuaternion();
            DVector3 p = new DVector3();
            for (int k = 0; k < 4; k++) {
                dQMultiply0(qr, q1[k % 2], q2[k / 2]);
                for (int j = 0; j < 3; j++) {
                    p.eqDiff(triangle[j], pos);
                    dQuatTransform(qr, p, perturbed);
                    perturbed.add(pos);
                    c2.vertices[j].set(perturbed.get0(), perturbed.get1(), perturbed.get2());
                }
                setObjPosToTriangleCenter(c2);
                if (ccdCollide(o1, o2, flags, perturbedContacts, c1, ccdSupportConvex, ccdCenter, c2, ccdSupportTriangle, ccdCenter) == 1) {
                    DContactGeom perturbedContact = perturbedContacts.get();
                    perturbedContact.side2 = contact.side2;
                    if (correctContactNormal(c2, perturbedContact, triangle, o2, indices)) {
                        contactcount = addUniqueContact(contacts, perturbedContact, contactcount, maxcontacts);
                    }
                }
            }
            return contactcount;
        }
    }

    ;

    private static final ccd_support_fn ccdSupportTriangle = new ccd_support_fn() {
        @Override
        public void run(Object obj, ccd_vec3_t _dir, ccd_vec3_t v) {
            final ccd_triangle_t o = (ccd_triangle_t) obj;
            double maxdot = -CCD_REAL_MAX;
            for (int i = 0; i < 3; i++) {
                double dot = ccdVec3Dot(_dir, o.vertices[i]);
                if (dot > maxdot) {
                    ccdVec3Copy(v, o.vertices[i]);
                    maxdot = dot;
                }
            }
        }
    };

}
